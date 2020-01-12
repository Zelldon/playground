package de.zell;

import io.atomix.protocols.raft.partition.impl.RaftNamespaces;
import io.atomix.protocols.raft.storage.log.RaftLog;
import io.atomix.protocols.raft.zeebe.ZeebeEntry;
import io.atomix.protocols.raft.zeebe.ZeebeLogAppender;
import io.atomix.storage.journal.Indexed;
import io.zeebe.logstreams.impl.log.LogStreamBuilderImpl;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.logstreams.log.LogStreamReader;
import io.zeebe.logstreams.storage.atomix.AtomixLogStorage;
import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.ActorScheduler.ActorSchedulerBuilder;
import io.zeebe.util.sched.future.CompletableActorFuture;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LogReader extends Actor {

  private final String path;
  private final ActorScheduler actorScheduler;

  public LogReader(ActorScheduler actorScheduler, String path) {
    this.actorScheduler = actorScheduler;
    this.path = path;
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

    actor.call(() -> {

      final var resourceDir = new File(LogReader.class.getResource("/").getFile());
      final var raftLog = RaftLog.builder()
          .withDirectory(resourceDir)
          .withName("example")
          .withNamespace(RaftNamespaces.RAFT_STORAGE)
          .withMaxEntrySize(4 * 1024 * 1024)
          .withMaxSegmentSize(512 * 1024 * 1024)
          .build();

      System.out.println("Log build");
      final var atomixLogStorage = new AtomixLogStorage(raftLog::openReader,
          (position) -> CompletableFuture.completedFuture(null)
          , () -> Optional.of(new NoopAppender()));
      new LogStreamBuilderImpl().withActorScheduler(actorScheduler).withLogStorage(atomixLogStorage)
          .buildAsync().onComplete((logStream, t) -> {

            logStream.newLogStreamReader().onComplete((reader, t2) -> {

              System.out.println("seek to end");
              reader.seekToEnd();

              System.out.println(reader.getPosition());
              future.complete(null);

            });
          });

    }).join();
    return future;
  }

  private class NoopAppender implements ZeebeLogAppender {

    @Override
    public void appendEntry(long l, long l1, ByteBuffer byteBuffer, AppendListener appendListener) {

    }
  }
}
