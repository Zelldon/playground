package io.zeebe.tools.inspector;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.DefaultZeebeDbFactory;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.Incident;
import io.zeebe.util.buffer.BufferUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Inspector {

  private static final Logger LOGGER = LoggerFactory.getLogger(Inspector.class);

  private static final String PARTITIONS_FOLDER = "data/raft-partition/partitions";
  private static final String DB_FOLDER = "runtime";

  private static final Map<String, String> USAGE_CMD = Map.of("incident", "get information about incidents",
      "blacklist", "get information about blacklisted instances");
  private static final Map<String, EntityInspection> COMMAND_FUNCTIONS = Map.of("incident", new IncidentInspection());
  private static final Map<String, Function<EntityInspection, String>> SUB_COMMAND_FUNCTIONS =
      Map.of(
      "list", Inspector::listEntities);
//      "entity", new IncidentInspection());

  private final Path rootDirectory;

  public Inspector(final Path rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  private static void printUsage(String[] args) {
    var builder = new StringBuilder("Unexpected usage. Couldn't map given parameters '")
        .append(Arrays.toString(args))
        .append('\'')
        .append('\n')
        .append("Expected usage: java -jar inspector.jar <pathToPartition> <command>")
        .append("\nCommand:");

    for (var entry : USAGE_CMD.entrySet()) {
      builder.append("\n\t- ")
          .append(entry.getKey())
          .append("\t\t\t")
          .append(entry.getValue());
    }

    LOGGER.warn(builder.toString());
  }

  private static void ensureCorrectUsage(String[] args) {
    if (args.length < 3) {
      printUsage(args);
      System.exit(1);
    }

    final String dir = args[0];
    final var partitionsDirectory = Path.of(dir);
    if (!Files.exists(partitionsDirectory)) {
      LOGGER.error("Root directory does not exist: {}", partitionsDirectory.toAbsolutePath());
      printUsage(args);
      System.exit(1);
    }

    try {
      final var partition = partitionsDirectory.getFileName().toString();
      Integer.valueOf(partition);
    } catch (NumberFormatException nfe) {
      LOGGER.error("The path must point to the partitions directory.");
      printUsage(args);
      System.exit(1);
    }

    final var command = args[1];
    if (!COMMAND_FUNCTIONS.containsKey(command)) {
      printUsage(args);
      System.exit(1);
    }

    final var subCommand = args[2];
    if (!SUB_COMMAND_FUNCTIONS.containsKey(subCommand)) {
      printUsage(args);
      System.exit(1);
    }
  }

  public static void main(String[] args) throws Exception {
    LOGGER.info("Zeebe Inspector \uD83D\uDD0E");

    ensureCorrectUsage(args);

    final var dir = args[0];
    final var partitionsDir = Path.of(dir);
    final var command = args[1];
    LOGGER.info("Partitions directory: {}", partitionsDir);

    final var partition = partitionsDir.getFileName().toString();
    final int partitionId = Integer.valueOf(partition);
    LOGGER.info("Partition: {}", partitionId);

    final var entityInspection = COMMAND_FUNCTIONS.get(command);
    ZeebeDb<ZbColumnFamilies> zeebeDb = null;
    try {
      final var dbDirectory = partitionsDir.resolve(DB_FOLDER);
      zeebeDb = openZeebeDb(dbDirectory);
      final var state = new ZeebeState(zeebeDb, partitionId, zeebeDb.createContext());
      entityInspection.use(state);

      final var subCommand = SUB_COMMAND_FUNCTIONS.get(args[2]);
      LOGGER.info(subCommand.apply(entityInspection));
    } finally {
      if (zeebeDb != null) {
        zeebeDb.close();
      }
    }
  }

  private static ZeebeDb<ZbColumnFamilies> openZeebeDb(Path directory) {
    LOGGER.info("Open database: {}", directory.toAbsolutePath());
      return
          DefaultZeebeDbFactory.DEFAULT_DB_FACTORY.createDb(directory.toFile());
  }

  private static String listEntities(EntityInspection entityInspection) {
    final var builder = new StringBuilder("\nList:");
    final var list = entityInspection.list();

    for (String entity : list) {
      builder.append("\n\t").append(entity);
    }

    return builder.toString();
  }

  private static String getEntity(EntityInspection entityInspection) {
//    final var builder = new StringBuilder("\nList:");
//    final var list = entityInspection.entity();
//
//    for (String entity : list) {
//      builder.append("\n\t").append(entity);
//    }
// Todo my idea seems not to work since we now need the key
    return "Entity";
  }

  private void printDeployedWorkflows(final PartitionState partition) {
    LOGGER.info("Deployed workflows:");
    partition.zeebeState.getWorkflowState().getWorkflows().forEach(workflow -> LOGGER
        .info("> Workflow[key: {}, version: {}, BPMN process id: '{}']", workflow.getKey(),
            workflow.getVersion(), BufferUtil.bufferAsString(workflow.getBpmnProcessId())));
  }

  private void printIncidents(final PartitionState partition) {

    LOGGER.info("Open incidents:");

    final DbLong incidentDbKey = new DbLong();
    final ColumnFamily<DbLong, Incident> incidentColumnFamily = partition.zeebeDb
        .createColumnFamily(ZbColumnFamilies.INCIDENTS, partition.dbContext, incidentDbKey,
            new Incident());

    incidentColumnFamily.forEach((key, incident) -> {

      final var incidentKey = key.getValue();
      final var incidentRecord = incident.getRecord();

      LOGGER.info(
          "> Incident[key: {}, workflow-instance: {}, BPMN process id: '{}', error type: {}, error-message: '{}']",
          incidentKey,
          incidentRecord.getWorkflowInstanceKey(), incidentRecord.getBpmnProcessId(),
          incidentRecord.getErrorType(), incidentRecord.getErrorMessage());
    });

  }

  private void printBlacklist(final PartitionState partition) {

    final var elementInstanceState = partition.zeebeState.getWorkflowState()
        .getElementInstanceState();

    final var blacklistColumnFamily = partition.zeebeDb
        .createColumnFamily(ZbColumnFamilies.BLACKLIST, partition.dbContext, new DbLong(),
            DbNil.INSTANCE);

    LOGGER.info("Workflow Instances on the Backlist:");

    blacklistColumnFamily.forEach((key, nil) -> {
      final var workflowInstanceKey = key.getValue();

      final var workflowInstance = elementInstanceState.getInstance(workflowInstanceKey);
      final var bpmnProcessId = workflowInstance.getValue()
          .getBpmnProcessId();

      LOGGER.info("> Workflow Instance[key: {}, BPMN process id: '{}']", workflowInstanceKey,
          bpmnProcessId);
    });
  }

  private static class PartitionState {

    private final ZeebeDb<ZbColumnFamilies> zeebeDb;
    private final ZeebeState zeebeState;
    private final DbContext dbContext;

    private PartitionState(
        final ZeebeDb<ZbColumnFamilies> zeebeDb, final ZeebeState zeebeState,
        final DbContext dbContext) {
      this.zeebeDb = zeebeDb;
      this.zeebeState = zeebeState;
      this.dbContext = dbContext;
    }
  }

}
