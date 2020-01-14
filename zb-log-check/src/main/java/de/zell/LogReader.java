package de.zell;

import io.atomix.protocols.raft.partition.impl.RaftNamespaces;
import io.atomix.protocols.raft.storage.log.RaftLog;
import io.atomix.protocols.raft.zeebe.ZeebeLogAppender;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.JournalReader.Mode;
import io.zeebe.logstreams.impl.log.LogStreamBuilderImpl;
import io.zeebe.logstreams.storage.atomix.AtomixLogStorage;
import io.zeebe.protocol.Protocol;
import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.future.CompletableActorFuture;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LogReader extends Actor {

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RED = "\u001B[31m";

  private final String path;
  private final ActorScheduler actorScheduler;
  private final String partitionName;
  private final int partitionId;

  public LogReader(
      ActorScheduler actorScheduler, String path, String partitionName, int partitionId) {
    this.actorScheduler = actorScheduler;
    this.path = path;
    this.partitionName = partitionName;
    this.partitionId = partitionId;
  }

  @Override
  protected void onActorStarting() {
    System.out.println("onActorStarting");
    super.onActorStarting();
  }

  @Override
  protected void onActorStarted() {
    System.out.println("onActorStarted");
    super.onActorStarted();
  }

  @Override
  protected void onActorClosing() {
    super.onActorClosing();
  }

  @Override
  protected void onActorClosed() {
    super.onActorClosed();
  }

  @Override
  protected void onActorCloseRequested() {
    super.onActorCloseRequested();
  }

  protected CompletableActorFuture<Void> open() {
    final var future = new CompletableActorFuture<Void>();

    actor
        .call(
            () -> {
              final var resourceDir = new File(path);
              final var raftLog =
                  RaftLog.builder()
                      .withDirectory(resourceDir)
                      .withName(partitionName)
                      .withNamespace(RaftNamespaces.RAFT_STORAGE)
                      .withMaxEntrySize(4 * 1024 * 1024)
                      .withMaxSegmentSize(128 * 1024 * 1024)
                      .withStorageLevel(StorageLevel.DISK)
                      .build();

              System.out.println("Log build");
              final var atomixLogStorage =
                  new AtomixLogStorage(
                      (idx, mode) -> {
                        return raftLog.openReader(idx, Mode.ALL);
                      },
                      (position) -> CompletableFuture.completedFuture(null),
                      () -> Optional.of(new NoopAppender()));
              new LogStreamBuilderImpl()
                  .withActorScheduler(actorScheduler)
                  .withLogStorage(atomixLogStorage)
                  .withLogName(partitionName)
                  .withPartitionId(partitionId)
                  .buildAsync()
                  .onComplete(
                      (logStream, t) -> {
                        logStream
                            .newLogStreamReader()
                            .onComplete(
                                (reader, t2) -> {
                                  System.out.println("Scan log...");
                                  reader.seekToFirstEvent();

                                  long low = Long.MAX_VALUE;
                                  long high = Long.MIN_VALUE;
                                  long lastPosition = 0;
                                  int eventCount = 0;
                                  boolean inconsistentLog = false;

                                  while (reader.hasNext()) {
                                    final var next = reader.next();

                                    final var position = next.getPosition();


                                    if (lastPosition > position) {
                                      inconsistentLog = true;
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

                                    if (position < low)
                                    {
                                      low = position;
                                    } else if (position > high) {
                                      high = position;
                                    }


                                    lastPosition = position;
                                    eventCount++;

                                  }
                                  System.out.println("Scan finished");
                                  if (inconsistentLog)
                                  {
                                    System.out.println(ANSI_RED + "LOG IS INCONSISTENT!" + ANSI_RESET);
                                  }
                                  else
                                  {
                                    System.out.println(ANSI_GREEN + "LOG IS CONSISTENT." + ANSI_RESET);
                                  }
                                  System.out.println("Lowest position: " + low);
                                  System.out.println("Highest position: " + high);
                                  System.out.println("Events: " + eventCount);

                                  future.complete(null);
                                });
                      });
            })
        .join();
    return future;
  }

  private class NoopAppender implements ZeebeLogAppender {

    @Override
    public void appendEntry(
        long l, long l1, ByteBuffer byteBuffer, AppendListener appendListener) {}
  }
}
