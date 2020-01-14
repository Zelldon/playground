package de.zell;

import io.zeebe.util.sched.ActorScheduler.ActorSchedulerBuilder;

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
            "/home/zell/goPath/src/github.com/Zelldon/playground/data/zeebe-0",
            "raft-partition", 3);
        actorScheduler.submitActor(actor).join();


        actor.scan().join();


    }
}
