package io.zeebe.tools.inspector;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.db.impl.DbLong;
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

  private final Path rootDirectory;

  public Inspector(final Path rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  private static void printUsage(String[] args) {
    var builder = new StringBuilder("Unexpected usage. Couldn't map given parameters '")
        .append(Arrays.toString(args))
        .append('\'')
        .append('\n')
        .append("Expected usage: java -jar inspector.jar <path> <command>")
        .append("\nCommand:");

    for (var entry : USAGE_CMD.entrySet()) {
      builder.append("\n\t- ")
          .append(entry.getKey())
          .append("\t\t\t")
          .append(entry.getValue());
    }

    LOGGER.warn(builder.toString());
  }

  public static void main(String[] args) {

    LOGGER.info("Zeebe Inspector \uD83D\uDD0E");

    if (args.length < 2) {
      printUsage(args);
      System.exit(1);
    }

    final String dir = args[0];
    final var root = Path.of(dir);
    if (!Files.exists(root)) {
      LOGGER.error("Root directory does not exist: {}", root.toAbsolutePath());
      System.exit(1);
    }

    final var command = args[1];
    if (!COMMAND_FUNCTIONS.containsKey(command)) {
      printUsage(args);
      System.exit(1);
    }

    LOGGER.info("Root directory: {}", root.toAbsolutePath());

    final var partitionsDir = root.resolve(PARTITIONS_FOLDER);
    LOGGER.info("Partitions directory: {}", partitionsDir.toAbsolutePath());

    final var inspector = new Inspector(root);

    final var states = inspector.openState(partitionsDir);

    states.forEach(inspector::printDeployedWorkflows);

    states.forEach(inspector::printIncidents);

    states.forEach(partition -> {
      try {
        partition.zeebeDb.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public List<PartitionState> openState(Path partitionsDirectory) {
    try {
      return Files.list(partitionsDirectory).map(partitionDir -> {
        final var partition = partitionDir.getFileName().toString();

        final var partitionId = Integer.valueOf(partition);

        LOGGER.info("Partition: {}", partitionId);

        final var dbDirectory = partitionDir.resolve(DB_FOLDER);

        final var zeebeDb = openZeebeDb(dbDirectory);
        final var dbContext = zeebeDb.createContext();
        final var zeebeState = new ZeebeState(partitionId, zeebeDb, dbContext);

        return new PartitionState(zeebeDb, zeebeState, dbContext);
      }).collect(Collectors.toList());

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private ZeebeDb<ZbColumnFamilies> openZeebeDb(Path directory) {
    LOGGER.info("Open database: {}", directory.toAbsolutePath());

    try {
      final ZeebeDb<ZbColumnFamilies> db =
          DefaultZeebeDbFactory.DEFAULT_DB_FACTORY.createDb(directory.toFile());

      return db;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
