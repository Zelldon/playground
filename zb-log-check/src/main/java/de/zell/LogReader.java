package de.zell;

import io.atomix.protocols.raft.partition.impl.RaftNamespaces;
import io.atomix.protocols.raft.storage.log.RaftLog;
import io.atomix.protocols.raft.zeebe.ZeebeLogAppender;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.JournalReader.Mode;
import io.zeebe.logstreams.impl.log.LogStreamBuilderImpl;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.logstreams.log.LogStreamReader;
import io.zeebe.logstreams.storage.atomix.AtomixLogStorage;
import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.future.ActorFuture;
import io.zeebe.util.sched.future.CompletableActorFuture;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class LogReader extends Actor {

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";

  private static final String DIR_FORMAT = "%s/data/%s/partitions/%d";
  private static final String PARTITION_NAME_FORMAT = "%s-partition-%d";

  private final String path;
  private final ActorScheduler actorScheduler;
  private final String partitionName;
  private final int partitionId;
  private ActorFuture<LogStream> logStreamFuture;

  public LogReader(
      ActorScheduler actorScheduler, String path, String partitionName, int partitionId) {
    this.actorScheduler = actorScheduler;
    this.path = String.format(DIR_FORMAT, path, partitionName, partitionId);
    this.partitionName = String.format(PARTITION_NAME_FORMAT, partitionName, partitionId);
    this.partitionId = partitionId;
  }


  @Override
  protected void onActorStarting() {
    final var resourceDir = new File(path);

    final var startTime = System.currentTimeMillis();
    final var raftLog =
        RaftLog.builder()
            .withDirectory(resourceDir)
            .withName(partitionName)
            .withNamespace(RaftNamespaces.RAFT_STORAGE)
            .withMaxEntrySize(4 * 1024 * 1024)
            .withMaxSegmentSize(512 * 1024 * 1024)
            .withStorageLevel(StorageLevel.DISK)
            .build();

    final var endtime = System.currentTimeMillis();
    System.out.println("Log opened in " + (endtime - startTime) + " ms");

    final var atomixLogStorage =
        new AtomixLogStorage(
            (idx, mode) -> {
              return raftLog.openReader(idx, Mode.ALL);
            },
            (position) -> CompletableFuture.completedFuture(null),
            () -> Optional.of(new NoopAppender()));

    logStreamFuture = new LogStreamBuilderImpl()
        .withActorScheduler(actorScheduler)
        .withLogStorage(atomixLogStorage)
        .withLogName(partitionName)
        .withPartitionId(partitionId)
        .buildAsync();
  }

  public CompletableActorFuture<Boolean> scan() {
    final var future = new CompletableActorFuture<Boolean>();

    actor
        .call(
            () -> {
              logStreamFuture.onComplete(
                      (logStream, t) -> {
                        if (t == null) {
                          logStream
                              .newLogStreamReader()
                              .onComplete(
                                  (reader, t2) -> {
                                    if (t2 == null) {
                                      future.complete(scanLog(reader));
                                    } else {
                                      future.completeExceptionally(t2);
                                    }
                                  });
                        }
                        else
                        {
                          future.completeExceptionally(t);
                        }
                      });
            });
    return future;
  }

  private boolean scanLog(LogStreamReader reader) {
    System.out.println("Scan log...");
    reader.seekToFirstEvent();

    final var validationContext = new ValidationContext();

    while (reader.hasNext()) {
      final var next = reader.next();
      final var position = next.getPosition();

      validationContext.onNextPosition(position);
    }

    System.out.println("Scan finished");

    return validationContext.finishValidation();
  }


  private static class ValidationContext {

    long low = Long.MAX_VALUE;
    long high = Long.MIN_VALUE;
    long lastPosition = 0;
    int eventCount = 0;
    boolean inconsistentLog = false;

    void onNextPosition(long position) {

      if (lastPosition > position) {
        inconsistentLog = true;
        onInconsistentLog(low, high, lastPosition, eventCount, position);
      }

      if (position < low)
      {
        low = position;
      } else if (position > high) {
        high = position;
      }


      lastPosition = position;
      eventCount++;
    }

    boolean finishValidation() {
      if (inconsistentLog)
      {
        System.out.println(ANSI_RED + "LOG IS INCONSISTENT!" + ANSI_RESET);
      }
      else
      {
        System.out.println(ANSI_GREEN + "LOG IS CONSISTENT." + ANSI_RESET);
      }
      System.out.println("Last position: " + lastPosition);
      System.out.println("Lowest position: " + low);
      System.out.println("Highest position: " + high);
      System.out.println("Events: " + eventCount);
      return inconsistentLog;
    }

    private static void onInconsistentLog(long low, long high, long lastPosition, int eventCount,
        long position) {
      System.out.println("===============");
      System.out.println("At idx " + eventCount);
      System.out.print("Current position " + position);
      System.out.print(" (Segment id " + (position >> 32) + " segment offset " + (int) position + ')');
      System.out.println();
      System.out.print("Is smaller then this last position " + lastPosition);
      System.out.print(" (Segment id " + (lastPosition >> 32) + " segment offset " + (int) lastPosition + ')');
      System.out.println();
      System.out.println("Current lowest " + low + " current highest " + high);
      System.out.println("===============");
    }

  }

  private class NoopAppender implements ZeebeLogAppender {

    @Override
    public void appendEntry(
        long l, long l1, ByteBuffer byteBuffer, AppendListener appendListener) {}
  }
}
