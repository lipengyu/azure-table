package com.yammer.collections.guava.azure.backup.tool;

import com.yammer.collections.guava.azure.backup.lib.Backup;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Date;

class ListBackupsCommand extends AbstractBackupCommand {
    private final Date thresholdDate;


    ListBackupsCommand(BackupConfiguration backupConfiguration, PrintStream infoStream, PrintStream errorStream, long timeSince) throws URISyntaxException, InvalidKeyException {
        super(backupConfiguration, infoStream, errorStream);
        this.thresholdDate = new Date(timeSince);
    }

    @Override
    public void run() throws Exception {
        Collection<Backup> backups = getBackupService().listAllBackups(thresholdDate);
        for (Backup backup : backups) {
            println(format(backup));
        }
    }
}
