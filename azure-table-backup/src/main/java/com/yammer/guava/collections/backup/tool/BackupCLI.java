package com.yammer.guava.collections.backup.tool;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Table;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.table.client.CloudTableClient;
import com.yammer.guava.collections.azure.StringAzureTable;
import com.yammer.guava.collections.backup.azure.AzureBackupTableFactory;
import com.yammer.guava.collections.backup.azure.AzureSourceTableFactory;
import com.yammer.guava.collections.backup.lib.*;
import org.apache.commons.cli.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

// TODO clean all bad backups (not COMPLTED)
public class BackupCLI {
    private static final Option CONFIG_FILE = new Option("cf", true, "azure account configuration");
    private static final Option BACKUP = new Option("b", "perform a backup");
    private static final Option LIST = new Option("l", "list existing backups"); // TODO list time, listall
    private static final Option DELETE = new Option("d", "delete old backups");
    private static final Option RESTORE = new Option("r", "restore"); // TODO backup name/time
    private static final OptionGroup BACKUP_OPTIONS = new OptionGroup().
            addOption(BACKUP).
            addOption(LIST).
            addOption(DELETE).
            addOption(RESTORE);
    private static final Options OPTIONS = setupOptions();

    private static Options setupOptions() {
        CONFIG_FILE.setRequired(true);
        BACKUP_OPTIONS.setRequired(true);

        return new Options().
                addOption(CONFIG_FILE).
                addOptionGroup(BACKUP_OPTIONS);
    }

    private static void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(BackupCLI.class.getName(), OPTIONS);
        System.exit(-1);
    }

    private static void printHelpAndExit(Throwable e) {
        System.err.println(e.getMessage());
        printHelpAndExit();
    }

    private static CommandLine parse(String args[]) {
        PosixParser parser = new PosixParser();
        try {
            return parser.parse(OPTIONS, args);
        } catch (ParseException e) {
            printHelpAndExit(e);
            throw Throwables.propagate(e); // never happens
        }
    }

    public static void main(String args[]) throws Exception {
        CommandLine commandLine = parse(args);

        BackupToolCommand backupCommand = null;
        final String configPath = commandLine.getOptionValue(CONFIG_FILE.getOpt());
        if (commandLine.hasOption(BACKUP.getOpt())) {
            backupCommand = new DoBackupCommand(configPath);
        } else if (commandLine.hasOption(LIST.getOpt())) {
            backupCommand = new ListBackups(configPath, 0); // TODO parse time
        } else if (commandLine.hasOption(DELETE.getOpt())) {
            backupCommand = new DeleteBackups(configPath, Long.MAX_VALUE); // TODO parse time
        } else {
            printHelpAndExit();
        }

        try {
            backupCommand.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        DEBUG_INFO();
    }

    private static void DEBUG_INFO() {
        //todo DELETE this method

        System.out.println("\n\n DEBUG INFO \n");

        BackupConfiguration configuration = new BackupConfiguration();
        configuration.setSourceTableName("backupToolValues");
        configuration.setSourceAccountName("secretietest");
        configuration.setSourceAccountKey("e5LnQoZei2cFH+56TFxDmO6AhnzMKill1NyVUs1M3R7OFNfCLnIGe17TLUex0mYYGQFjNvmArsLa8Iq3b0FNAg==");
        configuration.setBackupAccountName("secretietest");
        configuration.setBackupAccountKey("e5LnQoZei2cFH+56TFxDmO6AhnzMKill1NyVUs1M3R7OFNfCLnIGe17TLUex0mYYGQFjNvmArsLa8Iq3b0FNAg==");

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(configuration.getBackupConnectionString());
            CloudTableClient client = storageAccount.createCloudTableClient();
            System.out.println("\n\n === TABLES ===\n");
            for (String tableName : client.listTables()) {
                System.out.println(tableName);
            }

            System.out.println("\n\n === SOURCE ===\n");
            //CloudTable table = client.getTableReference(configuration.getSourceTableName());
            Table<String, String, String> azureTable = new StringAzureTable(configuration.getSourceTableName(), client);
            for (Table.Cell<String, String, String> tableCell : azureTable.cellSet()) {
                System.out.println(String.format("partKey=%s key=%s val=%s", tableCell.getRowKey(), tableCell.getColumnKey(), tableCell.getValue()));
            }

        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private static class DoBackupCommand extends BackupToolCommand {

        private DoBackupCommand(String configPath) throws Exception {
            super(configPath);
        }

        @Override
        public void run() throws Exception {
            BackupService.BackupResult result = getBackupService().backup();
            format(result.getBackup());
            Optional<Exception> failureCause = result.getFailureCause();
            if (failureCause.isPresent()) {
                throw failureCause.get();
            }

        }
    }

    private static class ListBackups extends BackupToolCommand {
        private final Date thresholdDate;


        private ListBackups(String configPath, long time) throws Exception {
            super(configPath);
            this.thresholdDate = new Date(time);
        }

        @Override
        public void run() throws Exception {
            Collection<Backup> backups = getBackupService().listAllBackups(thresholdDate);
            for (Backup backup : backups) {
                System.out.println(format(backup));
            }
        }
    }

    private static class DeleteBackups extends BackupToolCommand {
        private final Date thresholdDate;


        private DeleteBackups(String configPath, long time) throws Exception {
            super(configPath);
            this.thresholdDate = new Date(time);
        }

        @Override
        public void run() throws Exception {
            getBackupService().removeBackupsNotOlderThan(thresholdDate);
        }

    }

    private static abstract class BackupToolCommand {
        private final DateFormat dateFormat = new SimpleDateFormat();
        private final BackupService backupService;

        protected BackupToolCommand(String configPath) throws Exception {
            BackupConfiguration configuration = parseConfiguration(configPath);
            backupService = createBackupService(configuration);
        }

        private static BackupConfiguration parseConfiguration(String configPath) {
            // TODO add parsing
            BackupConfiguration configuration = new BackupConfiguration();
            configuration.setSourceTableName("backupToolValues");
            configuration.setSourceAccountName("secretietest");
            configuration.setSourceAccountKey("e5LnQoZei2cFH+56TFxDmO6AhnzMKill1NyVUs1M3R7OFNfCLnIGe17TLUex0mYYGQFjNvmArsLa8Iq3b0FNAg==");
            configuration.setBackupAccountName("secretietest");
            configuration.setBackupAccountKey("e5LnQoZei2cFH+56TFxDmO6AhnzMKill1NyVUs1M3R7OFNfCLnIGe17TLUex0mYYGQFjNvmArsLa8Iq3b0FNAg==");
            return configuration;
        }

        private static CloudTableClient createCloudTableClient(String connectionString) throws URISyntaxException, InvalidKeyException {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            return storageAccount.createCloudTableClient();
        }

        private static BackupTableFactory getBackupTableFactory(BackupConfiguration configuration) throws URISyntaxException, InvalidKeyException {
            CloudTableClient tableClient = createCloudTableClient(configuration.getBackupConnectionString());
            return new AzureBackupTableFactory(tableClient);
        }

        private static SourceTableFactory getSourceTableFactory(BackupConfiguration configuration) throws URISyntaxException, InvalidKeyException {
            CloudTableClient tableClient = createCloudTableClient(configuration.getSourceConnectionString());
            return new AzureSourceTableFactory(tableClient, configuration.getSourceTableName());
        }

        private static BackupService createBackupService(BackupConfiguration configuration) throws URISyntaxException, InvalidKeyException {
            BackupTableFactory backupTableFactory = getBackupTableFactory(configuration);
            SourceTableFactory sourceTableFactory = getSourceTableFactory(configuration);
            TableCopy<String, String, String> tableCopy = new TableCopy<>();
            return new BackupService(tableCopy, sourceTableFactory, backupTableFactory);
        }

        protected final BackupService getBackupService() {
            return backupService;
        }

        protected String format(Backup backup) {
            return String.format("Backup: NAME=%s DATE=%s STATUS=%s", backup.getName(), dateFormat.format(backup.getDate()), backup.getStatus());
        }

        public abstract void run() throws Exception;

    }

}
