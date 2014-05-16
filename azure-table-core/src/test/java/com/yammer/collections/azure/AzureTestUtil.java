/**
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS
 * OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under
 * the License.
 */
package com.yammer.collections.azure;


import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.microsoft.windowsazure.services.core.storage.utils.Base64;
import com.microsoft.windowsazure.services.table.client.TableOperation;
import com.microsoft.windowsazure.services.table.client.TableQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class AzureTestUtil {
    static final Function<Table.Cell<byte[], byte[], byte[]>, AzureEntity> ENCODE_CELL = new Function<Table.Cell<byte[], byte[], byte[]>, AzureEntity>() {
        @Override
        public AzureEntity apply(Table.Cell<byte[], byte[], byte[]> input) {
            return encodedEntity(input);
        }
    };

    private AzureTestUtil() {
    }

    @SafeVarargs
    static void setAzureTableToContain(String tableName,
                                       AzureTableRequestFactory azureTableRequestFactoryMock,
                                       AzureTableCloudClient azureTableCloudClientMock,
                                       Table.Cell<byte[], byte[], byte[]>... cells) throws StorageException {
        // retrieve setup in general
        TableOperation blanketRetrieveOperationMock = mock(TableOperation.class);
        when(azureTableRequestFactoryMock.retrieve(any(String.class), any(String.class))).thenReturn(blanketRetrieveOperationMock);


        // per entity setup
        TableQuery<AzureEntity> emptyQuery = mock(TableQuery.class);
        when(azureTableRequestFactoryMock.containsValueQuery(anyString(), anyString())).thenReturn(emptyQuery);
        when(azureTableCloudClientMock.execute(emptyQuery)).thenReturn(Collections.<AzureEntity>emptyList());
        Collection<AzureEntity> encodedStringEntities = Lists.newArrayList();
        for (Table.Cell<byte[], byte[], byte[]> cell : cells) {
            encodedStringEntities.add(encodedEntity(cell));
            setAzureTableToRetrieve(tableName, azureTableRequestFactoryMock, azureTableCloudClientMock, cell);

            TableQuery<AzureEntity> valueQuery = mock(TableQuery.class);
            when(azureTableRequestFactoryMock.containsValueQuery(tableName, encode(cell.getValue()))).thenReturn(valueQuery);
            when(azureTableCloudClientMock.execute(valueQuery)).thenReturn(Collections.singleton(ENCODE_CELL.apply(cell)));
        }

        // select query
        TableQuery<AzureEntity> tableQuery = mock(TableQuery.class);
        when(azureTableRequestFactoryMock.selectAll(tableName)).thenReturn(tableQuery);
        when(azureTableCloudClientMock.execute(tableQuery)).thenReturn(encodedStringEntities);

        setupRowQueries(tableName, azureTableRequestFactoryMock, azureTableCloudClientMock, cells);
        setupColumnQueries(tableName, azureTableRequestFactoryMock, azureTableCloudClientMock, cells);
    }

    static String encode(byte[] bytesToBeEncoded) {
        return Base64.encode(bytesToBeEncoded);
    }

    static AzureEntity encodedEntity(Table.Cell<byte[], byte[], byte[]> unEncodedcell) {
        return new AzureEntity(encode(unEncodedcell.getRowKey()), encode(unEncodedcell.getColumnKey()), encode(unEncodedcell.getValue()));
    }

    private static void setAzureTableToRetrieve(
            String tableName,
            AzureTableRequestFactory azureTableRequestFactoryMock,
            AzureTableCloudClient azureTableCloudClientMock,
            Table.Cell<byte[], byte[], byte[]> cell) throws StorageException {
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(azureTableRequestFactoryMock.retrieve(encode(cell.getRowKey()), encode(cell.getColumnKey()))).thenReturn(retriveTableOperationMock);
        when(azureTableCloudClientMock.execute(tableName, retriveTableOperationMock)).thenReturn(encodedEntity(cell));
    }

    @SafeVarargs
    private static void setupRowQueries(String tableName,
                                        AzureTableRequestFactory azureTableRequestFactoryMock,
                                        AzureTableCloudClient azureTableCloudClientMock,
                                        Table.Cell<byte[], byte[], byte[]>... cells) {

        TableQuery<AzureEntity> emptyQueryMock = mock(TableQuery.class);
        when(azureTableRequestFactoryMock.selectAllForRow(anyString(), anyString())).thenReturn(emptyQueryMock);
        when(azureTableRequestFactoryMock.containsValueForRowQuery(anyString(), anyString(), anyString())).thenReturn(emptyQueryMock);
        when(azureTableCloudClientMock.execute(emptyQueryMock)).thenReturn(Collections.<AzureEntity>emptyList());

        Multimap<byte[], Table.Cell<byte[], byte[], byte[]>> rowCellMap = HashMultimap.create();
        for (Table.Cell<byte[], byte[], byte[]> cell : cells) {
            rowCellMap.put(cell.getRowKey(), cell);

            TableQuery<AzureEntity> rowValueQueryMock = mock(TableQuery.class);
            when(
                    azureTableRequestFactoryMock.containsValueForRowQuery(
                            tableName,
                            encode(cell.getRowKey()),
                            encode(cell.getValue())
                    )
            ).thenReturn(rowValueQueryMock);
            when(azureTableCloudClientMock.execute(rowValueQueryMock)).thenReturn(Collections.singletonList(ENCODE_CELL.apply(cell)));
        }

        for (Map.Entry<byte[], Collection<Table.Cell<byte[], byte[], byte[]>>> entry : rowCellMap.asMap().entrySet()) {
            // row query
            TableQuery<AzureEntity> rowQueryMock = mock(TableQuery.class);
            when(azureTableRequestFactoryMock.selectAllForRow(tableName, encode(entry.getKey()))).
                    thenReturn(rowQueryMock);
            when(azureTableCloudClientMock.execute(rowQueryMock)).thenReturn(Collections2.transform(entry.getValue(), ENCODE_CELL));
        }
    }

    @SafeVarargs
    private static void setupColumnQueries(String tableName,
                                           AzureTableRequestFactory azureTableRequestFactoryMock,
                                           AzureTableCloudClient azureTableCloudClientMock,
                                           Table.Cell<byte[], byte[], byte[]>... cells) {

        TableQuery<AzureEntity> emptyQueryMock = mock(TableQuery.class);
        when(azureTableRequestFactoryMock.selectAllForColumn(anyString(), anyString())).thenReturn(emptyQueryMock);
        when(azureTableRequestFactoryMock.containsValueForColumnQuery(anyString(), anyString(), anyString())).thenReturn(emptyQueryMock);
        when(azureTableCloudClientMock.execute(emptyQueryMock)).thenReturn(Collections.<AzureEntity>emptyList());

        Multimap<byte[], Table.Cell<byte[], byte[], byte[]>> columnCellMap = HashMultimap.create();
        for (Table.Cell<byte[], byte[], byte[]> cell : cells) {
            columnCellMap.put(cell.getColumnKey(), cell);

            TableQuery<AzureEntity> columnValueQueryMock = mock(TableQuery.class);
            when(
                    azureTableRequestFactoryMock.containsValueForColumnQuery(
                            tableName,
                            encode(cell.getColumnKey()),
                            encode(cell.getValue())
                    )
            ).thenReturn(columnValueQueryMock);
            when(azureTableCloudClientMock.execute(columnValueQueryMock)).thenReturn(Collections.singletonList(ENCODE_CELL.apply(cell)));
        }

        for (Map.Entry<byte[], Collection<Table.Cell<byte[], byte[], byte[]>>> entry : columnCellMap.asMap().entrySet()) {
            // row query
            TableQuery<AzureEntity> columnQueryMock = mock(TableQuery.class);
            when(azureTableRequestFactoryMock.selectAllForColumn(tableName, encode(entry.getKey()))).
                    thenReturn(columnQueryMock);
            when(azureTableCloudClientMock.execute(columnQueryMock)).thenReturn(Collections2.transform(entry.getValue(), ENCODE_CELL));
        }
    }

}
