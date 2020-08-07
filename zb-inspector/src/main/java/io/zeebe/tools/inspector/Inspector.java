package io.zeebe.tools.inspector;

import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.DefaultZeebeDbFactory;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.util.buffer.BufferUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Inspector {

  private static final Logger LOGGER = LoggerFactory.getLogger(Inspector.class);

  private static final String PARTITIONS_FOLDER = "data/raft-partition/partitions";
  private static final String DB_FOLDER = "runtime";

  private final Path rootDirectory;

  public Inspector(final Path rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public static void main(String[] args) {

    LOGGER.info("Zeebe Inspector \uD83D\uDD0E");

    if (args.length < 1) {
      LOGGER.error("Missing argument.");
      System.exit(1);
    }

    final String dir = args[0];

    final var root = Path.of(dir);
    if (!Files.exists(root)) {
      LOGGER.error("Root directory does not exist: {}", root.toAbsolutePath());
      System.exit(1);
    }

    LOGGER.info("Root directory: {}", root.toAbsolutePath());

    final var partitionsDir = root.resolve(PARTITIONS_FOLDER);
    LOGGER.info("Partitions directory: {}", partitionsDir.toAbsolutePath());

    final var inspector = new Inspector(root);

    final var states = inspector.openState(partitionsDir);

    states.forEach(inspector::printDeployedWorkflows);

    // TODO: close db
  }

  public List<ZeebeState> openState(Path partitionsDirectory) {
    try {
      return Files.list(partitionsDirectory).map(partitionDir -> {
        final var partition = partitionDir.getFileName().toString();

        final var partitionId = Integer.valueOf(partition);

        LOGGER.info("Partition: {}", partitionId);

        final var dbDirectory = partitionDir.resolve(DB_FOLDER);

        final var zeebeDb = openZeebeDb(dbDirectory);
        final var zeebeState = openState(zeebeDb, partitionId);

        return zeebeState;
      }).collect(Collectors.toList());

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public ZeebeState openState(ZeebeDb<ZbColumnFamilies> zeebeDb, int partitionId) {
    final var dbContext = zeebeDb.createContext();
    final var zeebeState = new ZeebeState(partitionId, zeebeDb, dbContext);
    return zeebeState;
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

  private void printDeployedWorkflows(final ZeebeState zeebeState) {
    LOGGER.info("Deployed workflows:");
    zeebeState.getWorkflowState().getWorkflows().forEach(workflow -> LOGGER
        .info("> Workflow[key: {}, version: {}, BPMN process id: {}]", workflow.getKey(),
            workflow.getVersion(), BufferUtil.bufferAsString(workflow.getBpmnProcessId())));
  }

}
