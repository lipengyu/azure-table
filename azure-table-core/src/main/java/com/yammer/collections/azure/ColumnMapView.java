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
import com.google.common.collect.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/* package */
class ColumnMapView<R, C, V> implements Map<C, Map<R, V>> {
    private final Table<R, C, V> backingTable;
    private final Function<C, Map<R, V>> valueCreator;
    private final Function<C, Entry<C, Map<R, V>>> entryConstructor;

    ColumnMapView(final Table<R, C, V> backingTable) {
        this.backingTable = backingTable;
        valueCreator = new Function<C, Map<R, V>>() {
            @Override
            public Map<R, V> apply(C key) {
                return backingTable.column(key);
            }
        };
        entryConstructor = new

                Function<C, Entry<C, Map<R, V>>>() {
                    @Override
                    public Entry<C, Map<R, V>> apply(C input) {
                        return new ColumnMapViewEntry<>(backingTable, ColumnMapView.this, input);
                    }
                };
    }

    @Override
    public int size() {
        return backingTable.columnKeySet().size();
    }

    @Override
    public boolean isEmpty() {
        return backingTable.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backingTable.containsColumn(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof Entry)) {
            return false;
        }

        Entry<?, ?> entry = (Entry<?, ?>) value;
        try {
            return backingTable.row((R) entry.getKey()).containsValue(entry.getValue());
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @Override
    public Map<R, V> get(Object key) {
        if(key == null) {
            return null;
        }
        try {
            Map<R, V> mapForRow = backingTable.column((C) key);
            return mapForRow.isEmpty() ? null : mapForRow;
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    /**
     * This methods breaks the contract in that the returned value is not the previous
     * value, but the current value. This is because this class is only a view of a remote
     * data store.
     */
    @Override
    public Map<R, V> put(C key, Map<R, V> value) {
        checkNotNull(key);
        checkNotNull(value);
        Map<R, V> oldValue = get(key);
        oldValue.clear();
        if (!value.isEmpty()) {
            backingTable.column(key).putAll(value);
        }
        return oldValue;
    }

    /**
     * This methods breaks the contract in that the returned value is not the previous
     * value, but the current value. This is because this class is only a view of a remote
     * data store.
     */
    @Override
    public Map<R, V> remove(Object key) {
        if(key == null) {
            return null;
        }

        Map<R, V> oldValue = get(key);
        oldValue.clear();
        return oldValue;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void putAll(Map<? extends C, ? extends Map<R, V>> m) {
        checkNotNull(m);
        for (Entry<? extends C, ? extends Map<R, V>> entry : m.entrySet()) {
            backingTable.column(entry.getKey()).putAll(entry.getValue());
        }
    }

    @Override
    public void clear() {
        backingTable.clear();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<C> keySet() {
        return backingTable.columnKeySet();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Collection<Map<R, V>> values() {
        return Collections2.transform(
                keySet(),
                valueCreator
        );
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Entry<C, Map<R, V>>> entrySet() {
        return SetView.fromSetCollectionView(
                Collections2.transform(
                        keySet(),
                        entryConstructor
                )
        );
    }

    private static final class ColumnMapViewEntry<R, C, V> implements Entry<C, Map<R, V>> {
        private final Table<R, C, V> backingTable;
        private final Map<C, Map<R, V>> backingMap;
        private final C key;


        private ColumnMapViewEntry(Table<R, C, V> backingTable, Map<C, Map<R, V>> backingMap, C key) {
            this.backingTable = backingTable;
            this.backingMap = backingMap;
            this.key = key;
        }

        @Override
        public C getKey() {
            return key;
        }

        @Override
        public Map<R, V> getValue() {
            return backingTable.column(key);
        }

        @Override
        public Map<R, V> setValue(Map<R, V> value) {
            return backingMap.put(key, value);
        }
    }
}
