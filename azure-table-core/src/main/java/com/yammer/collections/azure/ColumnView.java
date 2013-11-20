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

class ColumnView implements Map<String, String> {
    private static final Function<AzureEntity, String> EXTRACT_COLUMN_KEY = new Function<AzureEntity, String>() {
        @Override
        public String apply(AzureEntity input) {
            return decode(input.getRowKey());
        }
    };
    private final Function<AzureEntity, Entry<String, String>> extractEntry;
    private final BaseAzureTable baseAzureTable;
    private final String rowKey;
    private final AzureTableCloudClient azureTableCloudClient;
    private final AzureTableRequestFactory azureTableRequestFactory;

    public ColumnView(final BaseAzureTable baseAzureTable,
                      final String rowKey,
                      AzureTableCloudClient azureTableCloudClient,
                      AzureTableRequestFactory azureTableRequestFactory) {
        this.baseAzureTable = baseAzureTable;
        this.rowKey = rowKey;
        this.azureTableCloudClient = azureTableCloudClient;
        this.azureTableRequestFactory = azureTableRequestFactory;
        extractEntry = new Function<AzureEntity, Entry<String, String>>() {
            @Override
            public Entry<String, String> apply(AzureEntity input) {
                return new ColumnMapEntry(rowKey, decode(input.getRowKey()), baseAzureTable);
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
        return baseAzureTable.contains(rowKey, key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }

        TableQuery<AzureEntity> valueQuery = azureTableRequestFactory.containsValueForRowQuery(baseAzureTable.getTableName(), encode(rowKey),
                encode((String) value));
        return azureTableCloudClient.execute(valueQuery).iterator().hasNext();
    }

    @Override
    public String get(Object key) {
        return baseAzureTable.get(rowKey, key);
    }

    @Override
    public String put(String key, String value) {
        return baseAzureTable.put(rowKey, key, value);
    }

    @Override
    public String remove(Object key) {
        return baseAzureTable.remove(rowKey, key);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for (Entry<? extends String, ? extends String> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        for (String columnKey : keySet()) {
            remove(columnKey);
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<String> keySet() {
        return SetView.fromSetCollectionView(
                new ColumnMapSetView<>(
                        baseAzureTable,
                        rowKey,
                        EXTRACT_COLUMN_KEY,
                        azureTableCloudClient,
                        azureTableRequestFactory
                )
        );
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Collection<String> values() {
        return new ColumnMapSetView<>(baseAzureTable, rowKey, EXTRACT_VALUE, azureTableCloudClient, azureTableRequestFactory);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Entry<String, String>> entrySet() {
        return SetView.fromSetCollectionView(
                new ColumnMapSetView<>(baseAzureTable, rowKey, extractEntry, azureTableCloudClient, azureTableRequestFactory)
        );
    }

    private static class ColumnMapEntry implements Entry<String, String> {
        private final String columnKey;
        private final String rowKey;
        private final BaseAzureTable azureTable;

        private ColumnMapEntry(String rowKey, String columnKey, BaseAzureTable azureTable) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.azureTable = azureTable;
        }

        @Override
        public String getKey() {
            return columnKey;
        }

        @Override
        public String getValue() {
            return azureTable.get(rowKey, columnKey);
        }

        @Override
        public String setValue(String value) {
            return azureTable.put(rowKey, columnKey, value);
        }
    }

    private static class ColumnMapSetView<E> extends AbstractCollectionView<E> {
        private final BaseAzureTable baseAzureTable;
        private final String rowKey;
        private final AzureTableCloudClient azureTableCloudClient;
        private final AzureTableRequestFactory azureTableRequestFactory;

        public ColumnMapSetView(
                BaseAzureTable baseAzureTable,
                String rowKey,
                Function<AzureEntity, E> typeExtractor,
                AzureTableCloudClient azureTableCloudClient,
                AzureTableRequestFactory azureTableRequestFactory) {
            super(typeExtractor);
            this.baseAzureTable = baseAzureTable;
            this.rowKey = rowKey;
            this.azureTableCloudClient = azureTableCloudClient;
            this.azureTableRequestFactory = azureTableRequestFactory;
        }

        @Override
        protected Iterable<AzureEntity> getBackingIterable() {
            TableQuery<AzureEntity> selectAllForRowQuery = azureTableRequestFactory.selectAllForRow(baseAzureTable.getTableName(), encode(rowKey));
            return azureTableCloudClient.execute(selectAllForRowQuery);
        }
    }
}
