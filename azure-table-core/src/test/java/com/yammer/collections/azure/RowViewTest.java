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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"InstanceVariableMayNotBeInitialized", "SuspiciousMethodCalls"})
@RunWith(MockitoJUnitRunner.class)
public class RowViewTest {
    private static final byte[] COLUMN_KEY = "columnKey".getBytes();
    private static final byte[] ROW_KEY_1 = "rowKey1".getBytes();
    private static final byte[] ROW_KEY_2 = "rowKey2".getBytes();
    private static final byte[] VALUE_1 = "value1".getBytes();
    private static final byte[] VALUE_2 = "value2".getBytes();
    private static final byte[] RET_VALUE = "ret_value".getBytes();
    private static final byte[] OTHER_ROW_KEY = "otherRow".getBytes();
    private static final byte[] OTHER_COLUMN_KEY = "otherKey".getBytes();
    private static final byte[] OTHER_VALUE = "otherValue".getBytes();
    private static final String TABLE_NAME = "table";
    private static final Table.Cell<byte[], byte[], byte[]> CELL_1 = Tables.immutableCell(ROW_KEY_1, COLUMN_KEY, VALUE_1);
    private static final Table.Cell<byte[], byte[], byte[]> CELL_2 = Tables.immutableCell(ROW_KEY_2, COLUMN_KEY, VALUE_2);
    private static final Table.Cell<byte[], byte[], byte[]> CELL_WITH_OTHER_COLUMN_KEY = Tables.immutableCell(OTHER_ROW_KEY, OTHER_COLUMN_KEY, OTHER_VALUE);
    private static final Function<Map.Entry, TestMapEntry> MAP_TO_ENTRIES = new Function<Map.Entry, TestMapEntry>() {
        @SuppressWarnings("ClassEscapesDefinedScope")
        @Override
        public TestMapEntry apply(Map.Entry input) {
            return new TestMapEntry(input);
        }
    };
    @Mock
    private AzureTableCloudClient azureTableCloudClientMock;
    @Mock
    private AzureTableRequestFactory azureTableRequestFactoryMock;
    @Mock
    private BaseAzureTable baseAzureTable;
    private RowView rowView;

    @Before
    public void setUp() {
        when(baseAzureTable.getTableName()).thenReturn(TABLE_NAME);
        rowView = new RowView(baseAzureTable, COLUMN_KEY, azureTableCloudClientMock, azureTableRequestFactoryMock);
    }

    @Test
    public void put_delegates_to_table() {
        when(baseAzureTable.put(ROW_KEY_1, COLUMN_KEY, VALUE_1)).thenReturn(RET_VALUE);

        assertThat(rowView.put(ROW_KEY_1, VALUE_1), is(equalTo(RET_VALUE)));
    }

    @Test
    public void get_delegates_to_table() {
        when(baseAzureTable.get(ROW_KEY_1, COLUMN_KEY)).thenReturn(VALUE_1);

        assertThat(rowView.get(ROW_KEY_1), is(equalTo(VALUE_1)));
    }

    @Test
    public void remove_delegates_to_table() {
        when(baseAzureTable.remove(ROW_KEY_1, COLUMN_KEY)).thenReturn(VALUE_1);

        assertThat(rowView.remove(ROW_KEY_1), is(equalTo(VALUE_1)));
    }

    @Test
    public void contains_key_delegates_to_table() {
        when(baseAzureTable.contains(ROW_KEY_1, COLUMN_KEY)).thenReturn(true);

        assertThat(rowView.containsKey(ROW_KEY_1), is(equalTo(true)));
    }

    @Test
    public void putAll_delegates_to_table() {
        rowView.putAll(
                ImmutableMap.of(
                        ROW_KEY_1, VALUE_1,
                        ROW_KEY_2, VALUE_2
                ));

        verify(baseAzureTable).put(ROW_KEY_1, COLUMN_KEY, VALUE_1);
        verify(baseAzureTable).put(ROW_KEY_2, COLUMN_KEY, VALUE_2);
    }

    @Test
    public void keySet_returns_contained_keys() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(rowView.keySet(), containsInAnyOrder(ROW_KEY_1, ROW_KEY_2));
    }

    @Test
    public void values_returns_contained_values() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(rowView.values(), containsInAnyOrder(VALUE_1, VALUE_2));
    }

    @Test
    public void entrySet_returns_contained_entries() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(
                Iterables.transform(rowView.entrySet(), MAP_TO_ENTRIES),
                containsInAnyOrder(
                        new TestMapEntry(ROW_KEY_1, VALUE_1),
                        new TestMapEntry(ROW_KEY_2, VALUE_2)
                ));
    }

    @Test
    public void setValue_on_entry_updates_backing_table() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);
        when(baseAzureTable.put(ROW_KEY_1, COLUMN_KEY, OTHER_VALUE)).thenReturn(RET_VALUE);
        when(baseAzureTable.put(ROW_KEY_2, COLUMN_KEY, OTHER_VALUE)).thenReturn(RET_VALUE);

        Map.Entry<byte[], byte[]> someEntry = rowView.entrySet().iterator().next();

        assertThat(someEntry.setValue(OTHER_VALUE), is(equalTo(RET_VALUE)));
        verify(baseAzureTable).put(someEntry.getKey(), COLUMN_KEY, OTHER_VALUE);
    }

    @Test
    public void size_returns_correct_value() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(rowView.size(), is(equalTo(2)));
    }

    @Test
    public void clear_deletes_values_from_key_set() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2, CELL_WITH_OTHER_COLUMN_KEY);

        rowView.clear();

        verify(baseAzureTable).remove(ROW_KEY_1, COLUMN_KEY);
        verify(baseAzureTable).remove(ROW_KEY_2, COLUMN_KEY);
    }

    @Test
    public void isEmpty_returns_false_if_no_entires() throws StorageException {
        setAzureTableToContain(CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(rowView.isEmpty(), is(equalTo(true)));
    }

    @Test
    public void isEmpty_returns_true_if_there_are_entires() throws StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(rowView.isEmpty(), is(equalTo(false)));
    }

    @Test
    public void contains_value_returns_true_if_value_contains() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_WITH_OTHER_COLUMN_KEY);

        assertThat(rowView.containsValue(VALUE_1), is(equalTo(true)));
    }

    @Test
    public void contains_value_returns_false_if_does_not_contain_value_in_row() throws StorageException {
        setAzureTableToContain(Tables.immutableCell(ROW_KEY_1, OTHER_COLUMN_KEY, VALUE_1));

        assertThat(rowView.containsValue(VALUE_1), is(equalTo(false)));
    }

    @Test
    public void contains_value_returns_false_if_object_not_string() throws StorageException {
        setAzureTableToContain();

        assertThat(rowView.containsValue(new Object()), is(equalTo(false)));
    }


    //----------------------
    // Utilities
    //----------------------

    @SafeVarargs
    private final void setAzureTableToContain(Table.Cell<byte[], byte[], byte[]>... cells) throws StorageException {
        for (Table.Cell<byte[], byte[], byte[]> cell : cells) {
            when(baseAzureTable.get(cell.getRowKey(), cell.getColumnKey())).thenReturn(cell.getValue());
        }
        AzureTestUtil.setAzureTableToContain(TABLE_NAME, azureTableRequestFactoryMock, azureTableCloudClientMock, cells);
    }

    private static class TestMapEntry implements Map.Entry<byte[], byte[]> {
        private final byte[] key;
        private final byte[] value;

        public TestMapEntry(Map.Entry<byte[], byte[]> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public TestMapEntry(byte[] key, byte[] value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public byte[] getKey() {
            return key;
        }

        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public byte[] setValue(byte[] value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestMapEntry that = (TestMapEntry) o;

            if (key != null ? !Arrays.equals(key, that.key) : that.key != null) return false;
            if (value != null ? !Arrays.equals(value, that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? Arrays.hashCode(key) : 0;
            result = 31 * result + (value != null ? Arrays.hashCode(value) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "[" + Arrays.toString(key) + "," + Arrays.toString(value) + "]";
        }
    }

}
