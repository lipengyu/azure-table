package com.yammer.collections.guava.azure.backup.tool;

import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.table.client.CloudTableClient;
import com.yammer.collections.guava.azure.backup.adapter.AzureBackupTableFactory;
import com.yammer.collections.guava.azure.backup.adapter.AzureSourceTableFactory;
import com.yammer.collections.guava.azure.backup.lib.*;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// TODO provide tests for all commands, requires decoupling from configuration and azure
abstract class AbstractBackupCommand implements BackupCommand {
    private final DateFormat dateFormat = new SimpleDateFormat();
    private final BackupService backupService;
    private final String backupName;
    private final PrintStream infoStream;
    private final PrintStream errorStrem;

    AbstractBackupCommand(BackupConfiguration backupConfiguration, PrintStream infoStream, PrintStream errorStrem) throws URISyntaxException,
            InvalidKeyException {
        this.infoStream = infoStream;
        this.errorStrem = errorStrem;
        backupName = backupConfiguration.getSourceTableName();
        backupService = createBackupService(backupConfiguration);
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

    protected String getBackupName() {
        return backupName;
    }

    protected String format(Backup backup) {
        return String.format("Backup: NAME=%s DATE=%s TIMESTAMP=%s STATUS=%s", backup.getName(), dateFormat.format(backup.getDate()),
                backup.getDate().getTime(), backup.getStatus());
    }

    public abstract void run() throws Exception;

    public final void println(String str) {
        infoStream.println(str);
    }

    public final void printErrorln(String str) {
        errorStrem.println(str);
    }

}
