package io.zeebe.tools.inspector;

import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.DefaultZeebeDbFactory;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Inspector {

  private static final Logger LOGGER = LoggerFactory.getLogger(Inspector.class);

  private static final String PARTITIONS_FOLDER = "data/raft-partition/partitions";
  private static final String DB_FOLDER = "runtime";

  private static final Map<String, String> USAGE_COMMAND =
      Map.of(
          "incident", "get information about incidents",
      "blacklist", "get information about blacklisted instances");

  private static final Map<String, String> USAGE_SUB_COMMAND =
      Map.of(
          "list", "get a list of existing entities",
          "entity <key>", "get information about a specificy entity - a separate key is needed.");

  private static final Map<String, EntityInspection> COMMAND_FUNCTIONS = Map.of("incident", new IncidentInspection());
  private static final Set<String> SUB_COMMAND_FUNCTIONS = Set.of("list", "entity");

  private static void printUsage(String[] args) {
    var builder = new StringBuilder("Unexpected usage. Couldn't map given parameters '")
        .append(Arrays.toString(args))
        .append('\'')
        .append('\n')
        .append("Expected usage: java -jar inspector.jar <pathToPartition> <command> <subcommand>")
        .append("\nCommand:");

    for (var entry : USAGE_COMMAND.entrySet()) {
      builder.append("\n\t- ")
          .append(entry.getKey())
          .append("\t\t\t")
          .append(entry.getValue());
    }

    builder.append("\nSubcommand:");

    for (var entry : USAGE_SUB_COMMAND.entrySet()) {
      builder.append("\n\t- ")
          .append(entry.getKey())
          .append("\t\t")
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
    if (!SUB_COMMAND_FUNCTIONS.contains(subCommand)) {
      printUsage(args);
      System.exit(1);
    }

    if (subCommand.equals("entity")) {
      if (args.length < 4) {
        LOGGER.error("The entity subcommand expects and key as additional paramter.");
        printUsage(args);
        System.exit(1);
      }

      final var key = args[3];
      try {
        Long.parseLong(key);
      } catch (NumberFormatException nfe) {
        LOGGER.error("The entity subcommand expects an long key as additional parameter.", nfe);
        printUsage(args);
        System.exit(1);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    LOGGER.info("Zeebe Inspector \uD83D\uDD0E");

    ensureCorrectUsage(args);

    final var dir = args[0];
    final var partitionsDir = Path.of(dir);
    LOGGER.info("Partitions directory: {}", partitionsDir);
    final var command = args[1];
    final var subCommand = args[2];

    final var partition = partitionsDir.getFileName().toString();
    final int partitionId = Integer.valueOf(partition);
    LOGGER.info("Partition: {}", partitionId);

    final var entityInspection = COMMAND_FUNCTIONS.get(command);
    ZeebeDb<ZbColumnFamilies> zeebeDb = null;
    try {
      final var dbDirectory = partitionsDir.resolve(DB_FOLDER);
      zeebeDb = openZeebeDb(dbDirectory);
      final var dbContext = zeebeDb.createContext();
      final var state = new ZeebeState(partitionId, zeebeDb, dbContext);

      final var partitionState = PartitionState.of(zeebeDb, state, dbContext);

      if (subCommand.equals("list"))
      {
        LOGGER.info(listEntities(partitionState, entityInspection));
      } else if (subCommand.equals("entity")){
        if (args.length < 4) {
          System.exit(1);
        }

        final var key = Long.parseLong(args[3]);
        LOGGER.info(getEntity(partitionState, entityInspection, key));
      }

      LOGGER.info("Bye...");
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

  private static String listEntities(PartitionState partitionState, EntityInspection entityInspection) {
    final var builder = new StringBuilder("\nList:");
    final var list = entityInspection.list(partitionState);

    for (String entity : list) {
      builder.append("\n\t").append(entity);
    }

    return builder.toString();
  }

  private static String getEntity(PartitionState partitionState, EntityInspection entityInspection, long key) {
    return entityInspection.entity(partitionState, key);
  }

}
