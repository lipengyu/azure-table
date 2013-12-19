package com.yammer.collections.guava.azure.backup.lib;

import com.google.common.collect.Table;

import java.util.Date;

public interface BackupTableFactory {

    public Table<String, Date, Backup.BackupStatus> getBackupListTable();

    Table<String, String, String> createBackupTable(Date backupDate, String backupName);

    void removeTable(Date backupDate, String backupName);

    Table<String, String, String> getBackupTable(Date date, String name);
}
