package com.yammer.collections.azure.backup.lib;


import com.google.common.collect.Table;


public interface SourceTableFactory {

    Table<String, String, String> getSourceTable();

    String getTableName();

    void clearSourceTable();
}