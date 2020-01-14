package de.zell;

import io.zeebe.util.sched.ActorScheduler.ActorSchedulerBuilder;
import java.io.File;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        final var actorScheduler = new ActorSchedulerBuilder().build();
        actorScheduler.start();

        final var actor = new LogReader(actorScheduler,
            App.class.getResource("/raft-partition/partitions/1").getFile(), "raft-partition-partition-1", 1);
        actorScheduler.submitActor(actor).join();


        actor.scan().join();


    }
}
