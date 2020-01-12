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

        final var actor = new LogReader(actorScheduler, "example.log");
        actorScheduler.submitActor(actor).join();


        actor.open().join();


    }
}
