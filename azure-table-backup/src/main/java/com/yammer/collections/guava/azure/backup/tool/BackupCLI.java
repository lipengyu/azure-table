package com.yammer.collections.guava.azure.backup.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class BackupCLI {
    private final Option CONFIG_FILE = OptionBuilder.
            withArgName("file").
            hasArg().
            withDescription("azure src and backup accounts configuration file").
            isRequired().
            create("cf");
    private final Option LIST = OptionBuilder.
            withArgName("timestamp").
            hasArg().
            withDescription("lists existing backups since timestamp").
            create("l");
    private final Option DELETE = OptionBuilder.
            withArgName("timestamp").
            hasArg().
            withDescription("deletes all backups until timestamp").
            create("d");
    private final Option RESTORE = OptionBuilder.
            withArgName("timestamp").
            hasArg().
            withDescription("restores the backup performed at the given timestamp").
            create("r");
    private final Option LIST_ALL = new Option("la", "list all backups");
    private final Option BACKUP = new Option("b", "perform a backup");
    private final Option DELETE_BAD_BACKUPS = new Option("db", "delete backups in bad state, i.e., not in COMPLETED");
    private final OptionGroup BACKUP_OPTIONS = new OptionGroup().
            addOption(BACKUP).
            addOption(LIST).
            addOption(LIST_ALL).
            addOption(DELETE).
            addOption(RESTORE).
            addOption(DELETE_BAD_BACKUPS);
    private final Options OPTIONS = setupOptions();

    private Options setupOptions() {
        CONFIG_FILE.setRequired(true);
        BACKUP_OPTIONS.setRequired(true);

        return new Options().
                addOption(CONFIG_FILE).
                addOptionGroup(BACKUP_OPTIONS);
    }

    private void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(BackupCLI.class.getName(), OPTIONS);
        System.exit(-1);
    }

    private void printHelpAndExit(Throwable e) {
        System.err.println(e.getMessage());
        printHelpAndExit();
    }

    private CommandLine parse(String args[]) {
        PosixParser parser = new PosixParser();
        try {
            return parser.parse(OPTIONS, args);
        } catch (ParseException e) {
            printHelpAndExit(e);
            throw Throwables.propagate(e); // never happens
        }
    }

    private Optional<AbstractBackupToolCommand> createBackupCommandFrom(CommandLine commandLine) throws Exception {
        AbstractBackupToolCommand backupCommand = null;
        final Printer stdPrinter = new StdOutputsPrinter();
        final BackupConfiguration backupConfiguration = getBackupConfiguration(commandLine);
        if (commandLine.hasOption(BACKUP.getOpt())) {
            backupCommand = new DoBackupCommand(backupConfiguration, stdPrinter);
        } else if (commandLine.hasOption(LIST.getOpt())) {
            long timeSince = parseTimestamp(commandLine.getOptionValue(LIST.getOpt()));
            backupCommand = new ListBackupsCommand(backupConfiguration, stdPrinter, timeSince);
        } else if (commandLine.hasOption(LIST_ALL.getOpt())) {
            backupCommand = new ListBackupsCommand(backupConfiguration, stdPrinter, 0);
        } else if (commandLine.hasOption(DELETE.getOpt())) {
            long timeTill = parseTimestamp(commandLine.getOptionValue(DELETE.getOpt()));
            backupCommand = new DeleteBackupsCommand(backupConfiguration, stdPrinter, timeTill);
        } else if (commandLine.hasOption(DELETE_BAD_BACKUPS.getOpt())) {
            backupCommand = new DeleteBadBackupsCommand(backupConfiguration, stdPrinter);
        } else if (commandLine.hasOption(RESTORE.getOpt())) {
            long backupTime = parseTimestamp(commandLine.getOptionValue(RESTORE.getOpt()));
            backupCommand = new RestoreCommand(backupConfiguration, stdPrinter, backupTime);
        }

        return Optional.fromNullable(backupCommand);
    }

    private void executeBackupCommand(AbstractBackupToolCommand backupCommand) {
        final long startTime = System.currentTimeMillis();
        long duration = 0;
        try {
            backupCommand.run();
            duration = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            duration = System.currentTimeMillis() - startTime;
            e.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("Running time was: " + duration + "[ms]");
        }
    }

    public void execute(String args[]) throws Exception {
        CommandLine commandLine = parse(args);
        Optional<AbstractBackupToolCommand> backupCommand = createBackupCommandFrom(commandLine);
        if(backupCommand.isPresent()) {
            executeBackupCommand(backupCommand.get());
        } else {
            printHelpAndExit();
        }
    }

    public static void main(String args[]) throws Exception {
        (new BackupCLI()).execute(args);
    }

    private BackupConfiguration getBackupConfiguration(CommandLine commandLine) throws IOException {
        final String configPath = commandLine.getOptionValue(CONFIG_FILE.getOpt());
        final File configurationFile = new File(configPath);
        final ObjectMapper configurationObjectMapper = new ObjectMapper(new YAMLFactory());
        final JsonNode node = configurationObjectMapper.readTree(configurationFile);
        return configurationObjectMapper.readValue(new TreeTraversingParser(node), BackupConfiguration.class);
    }

    private Long parseTimestamp(String data) {
        try {
            return Long.parseLong(data);
        } catch (NumberFormatException e) {
            System.err.println("Timestamp must be provided as a numeric value. Failed to read timestamp. " + e.getMessage());
            printHelpAndExit();
            return null;
        }
    }

    private final class StdOutputsPrinter implements Printer {

        @Override
        public void println(String string) {
            System.out.println(string);
        }

        @Override
        public void printErrorln(String string) {
            System.err.println(string);
        }
    }

}
