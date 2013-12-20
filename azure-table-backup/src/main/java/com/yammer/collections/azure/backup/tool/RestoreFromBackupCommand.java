package com.yammer.collections.azure.backup.tool;

import com.google.common.base.Optional;
import com.yammer.collections.azure.backup.lib.Backup;
import com.yammer.collections.azure.backup.lib.BackupService;
import com.yammer.collections.azure.backup.lib.TableCopyException;

import java.io.PrintStream;
import java.util.Date;

public class RestoreFromBackupCommand extends AbstractBackupCommand {
    private final Date backupTime;
    private final String backupName;


    public RestoreFromBackupCommand(BackupService backupService, String backupName, PrintStream infoStream, PrintStream errorStream, long backupTime) {
        super(backupService, infoStream, errorStream);
        this.backupName = backupName;
        this.backupTime = new Date(backupTime);
    }

    @Override
    public void unsafeRun() throws TableCopyException {
        Optional<Backup> backup = getBackupService().findBackup(backupName, backupTime);
        if (backup.isPresent()) {
            getBackupService().restore(backup.get());
            println("Restored backup: " + format(backup.get()));
        } else {
            printErrorln("No backup found for table=" + backupName + " at timestamp=" + backupTime.getTime());
        }
    }
}
