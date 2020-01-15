package de.zell;

import static java.lang.System.exit;

import io.zeebe.util.sched.ActorScheduler.ActorSchedulerBuilder;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LogVerificationMain
{
    private static final String PARTITIONS_DIRECTORY_FORMAT = "%s/data/%s/partitions/";

    public static void main( String[] args )
    {
        if (args.length != 1)
        {
            System.out.println("Expected to be called with one parameter, got " + args.length);
            System.out.println("Correct usage: java -jar LogVerificationMain PATH_TO_DATA_FOLDER");
            return;
        }

        final var pathToDataFolder = args[0];

        final var partitionsFolder = new File(String
            .format(PARTITIONS_DIRECTORY_FORMAT, pathToDataFolder, "raft-partition"));

        if (!partitionsFolder.exists()) {
            System.err.println("Tried to find partitions under " + partitionsFolder.getPath() + ", but directory doesn't exist.");
            return;
        }

        final var files = partitionsFolder.listFiles();

        if (files == null || files.length == 0) {
            System.err.println("Expected to find at least on partition under " + partitionsFolder.getPath() + ", but nothing was found.");
            return;
        }

        final var partitionDirectories = Arrays.stream(files).filter(f -> isInteger(f.getName()))
            .collect(Collectors.toList());

        if (partitionDirectories.isEmpty()) {
            System.err.println("Expected to find at least one partition directory under " + partitionsFolder.getPath() + ", but found only " + files);
            return;
        }

        final var inconsistentLog = verifyPartitions(pathToDataFolder, partitionDirectories);

        exit(inconsistentLog ? 1 : 0);
    }

    private static boolean verifyPartitions(String pathToDataFolder, List<File> partitionDirectories) {
        final var actorScheduler = new ActorSchedulerBuilder().build();
        actorScheduler.start();

        boolean inconsistentLog = false;
        for (var partitionDir : partitionDirectories) {
            System.out.println("Verify partition at " + partitionDir);
            final var logReader = new LogReader(actorScheduler,
                pathToDataFolder,
                "raft-partition", Integer.parseInt(partitionDir.getName()));
            actorScheduler.submitActor(logReader).join();

            inconsistentLog |= logReader.scan().join();
        }
        return inconsistentLog;
    }

    private static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
