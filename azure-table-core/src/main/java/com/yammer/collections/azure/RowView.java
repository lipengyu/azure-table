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
import com.microsoft.windowsazure.services.table.client.TableQuery;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.yammer.collections.azure.AzureEntityUtil.EXTRACT_VALUE;
import static com.yammer.collections.azure.AzureEntityUtil.decode;
import static com.yammer.collections.azure.AzureEntityUtil.encode;

class RowView implements Map<Bytes, Bytes> {
    private static final Function<AzureEntity, Bytes> EXTRACT_ROW_KEY = new Function<AzureEntity, Bytes>() {
        @Override
        public Bytes apply(AzureEntity input) {
            return decode(input.getPartitionKey());
        }
    };
    private final BaseAzureTable baseAzureTable;
    private final Bytes columnKey;
    private final AzureTableCloudClient azureTableCloudClient;
    private final AzureTableRequestFactory azureTableRequestFactory;
    private final Function<AzureEntity, Entry<Bytes, Bytes>> extractEntry;

    RowView(
            final BaseAzureTable baseAzureTable,
            final Bytes columnKey,
            AzureTableCloudClient azureTableCloudClient,
            AzureTableRequestFactory azureTableRequestFactory) {
        this.baseAzureTable = baseAzureTable;
        this.columnKey = columnKey;
        this.azureTableCloudClient = azureTableCloudClient;
        this.azureTableRequestFactory = azureTableRequestFactory;
        extractEntry = new Function<AzureEntity, Entry<Bytes, Bytes>>() {
            @Override
            public Entry<Bytes, Bytes> apply(AzureEntity input) {
                return new RowMapEntry(decode(input.getPartitionKey()), columnKey, baseAzureTable);
            }
        };
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return baseAzureTable.contains(key, columnKey);
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof Bytes)) {
            return false;
        }

        TableQuery<AzureEntity> valueQuery = azureTableRequestFactory.containsValueForColumnQuery(baseAzureTable.getTableName(), encode(columnKey),
                encode((Bytes) value));
        return azureTableCloudClient.execute(valueQuery).iterator().hasNext();
    }

    @Override
    public Bytes get(Object key) {
        return baseAzureTable.get(key, columnKey);
    }

    @Override
    public Bytes put(Bytes key, Bytes value) {
        return baseAzureTable.put(key, columnKey, value);
    }

    @Override
    public Bytes remove(Object key) {
        return baseAzureTable.remove(key, columnKey);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void putAll(Map<? extends Bytes, ? extends Bytes> m) {
        for (Entry<? extends Bytes, ? extends Bytes> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        for (Bytes rowKey : keySet()) {
            remove(rowKey);
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Bytes> keySet() {
        return SetView.fromSetCollectionView(
                new RowMapSetView<>(baseAzureTable, columnKey, EXTRACT_ROW_KEY, azureTableCloudClient, azureTableRequestFactory)
        );
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Collection<Bytes> values() {
        return new RowMapSetView<>(baseAzureTable, columnKey, EXTRACT_VALUE, azureTableCloudClient, azureTableRequestFactory);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Entry<Bytes, Bytes>> entrySet() {
        return SetView.fromSetCollectionView(
                new RowMapSetView<>(baseAzureTable, columnKey, extractEntry, azureTableCloudClient, azureTableRequestFactory)
        );
    }

    private static class RowMapEntry implements Entry<Bytes, Bytes> {
        private final Bytes columnKey;
        private final Bytes rowKey;
        private final BaseAzureTable azureTable;

        private RowMapEntry(Bytes rowKey, Bytes columnKey, BaseAzureTable azureTable) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.azureTable = azureTable;
        }

        @Override
        public Bytes getKey() {
            return rowKey;
        }

        @Override
        public Bytes getValue() {
            return azureTable.get(rowKey, columnKey);
        }

        @Override
        public Bytes setValue(Bytes value) {
            return azureTable.put(rowKey, columnKey, value);
        }
    }

    private static class RowMapSetView<E> extends AbstractCollectionView<E> {
        private final BaseAzureTable baseAzureTable;
        private final Bytes columnKey;
        private final AzureTableCloudClient azureTableCloudClient;
        private final AzureTableRequestFactory azureTableRequestFactory;

        public RowMapSetView(
                BaseAzureTable baseAzureTable,
                Bytes columnKey,
                Function<AzureEntity, E> typeExtractor,
                AzureTableCloudClient azureTableCloudClient,
                AzureTableRequestFactory azureTableRequestFactory) {
            super(typeExtractor);
            this.baseAzureTable = baseAzureTable;
            this.columnKey = columnKey;
            this.azureTableCloudClient = azureTableCloudClient;
            this.azureTableRequestFactory = azureTableRequestFactory;
        }

        @Override
        protected Iterable<AzureEntity> getBackingIterable() {
            TableQuery<AzureEntity> selectAllForRowQuery = azureTableRequestFactory.selectAllForColumn(baseAzureTable.getTableName(), encode(columnKey));
            return azureTableCloudClient.execute(selectAllForRowQuery);
        }
    }

}
