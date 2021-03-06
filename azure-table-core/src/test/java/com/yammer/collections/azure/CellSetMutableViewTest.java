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

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"ClassWithTooManyMethods", "InstanceVariableMayNotBeInitialized", "SuspiciousMethodCalls"})
@RunWith(MockitoJUnitRunner.class)
public class CellSetMutableViewTest {
    private static final Bytes ROW_KEY_1 = new Bytes("rown_name_1".getBytes());
    private static final Bytes ROW_KEY_2 = new Bytes("row_name_2".getBytes());
    private static final Bytes COLUMN_KEY_1 = new Bytes("column_key_1".getBytes());
    private static final Bytes COLUMN_KEY_2 = new Bytes("column_key_2".getBytes());
    private static final Bytes VALUE_1 = new Bytes("value1".getBytes());
    private static final Bytes VALUE_2 = new Bytes("value3".getBytes());
    private static final String TABLE_NAME = "secretie_table";
    private static final Table.Cell<Bytes, Bytes, Bytes> CELL_1 = Tables.immutableCell(ROW_KEY_1, COLUMN_KEY_1, VALUE_1);
    private static final Table.Cell<Bytes, Bytes, Bytes> CELL_2 = Tables.immutableCell(ROW_KEY_2, COLUMN_KEY_2, VALUE_2);
    @Mock
    private BaseAzureTable baseAzureTable;
    @Mock
    private AzureTableCloudClient azureTableCloudClientMock;
    @Mock
    private AzureTableRequestFactory azureTableRequestFactoryMock;
    private CellSetMutableView set;

    @Before
    public void setUp() {
        when(baseAzureTable.getTableName()).thenReturn(TABLE_NAME);
        set = new CellSetMutableView(baseAzureTable, azureTableCloudClientMock, azureTableRequestFactoryMock);
    }

    @Test
    public void size_returns_correct_value() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        assertThat(set.size(), is(equalTo(2)));
    }

    @Test
    public void is_returns_false_on_non_empty_set() throws StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(set.isEmpty(), is(equalTo(false)));
    }

    @Test
    public void is_returns_true_on_empty_set() throws StorageException {
        setAzureTableToContain();

        assertThat(set.isEmpty(), is(equalTo(true)));
    }

    @Test
    public void contains_on_non_table_cell_returns_false() {
        assertThat(set.contains(new Object()), is(equalTo(false)));
    }

    @Test
    public void contains_on_null_returns_false() {
        assertThat(set.contains(null), is(equalTo(false)));
    }

    @Test
    public void contains_delegates_to_table() {
        Object o1 = new Object();
        Object o2 = new Object();
        Table.Cell<Object, Object, Object> cell = Tables.immutableCell(o1, o2, new Object());
        when(baseAzureTable.contains(o1, o2)).thenReturn(true);

        assertThat(set.contains(cell), is(equalTo(true)));
    }

    @Test
    public void iterator_contains_contained_entities() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        assertThat(set, containsInAnyOrder(CELL_1, CELL_2));
    }

    @Test
    public void add_delegates_to_table() {
        set.add(CELL_1);

        verify(baseAzureTable).put(ROW_KEY_1, COLUMN_KEY_1, VALUE_1);
    }

    @Test(expected = NullPointerException.class)
    public void add_null_not_allowed() {
        set.add(null);
    }

    @Test
    public void when_value_existed_in_table_then_add_returns_false() throws StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(set.add(CELL_1), is(equalTo(false)));
    }

    @Test
    public void when_value_did_not_exist_in_table_then_add_returns_true() throws StorageException {
        setAzureTableToContain();

        assertThat(set.add(CELL_1), is(equalTo(true)));
    }

    @Test
    public void remove_delegates_to_table() {
        set.remove(CELL_1);

        verify(baseAzureTable).remove(ROW_KEY_1, COLUMN_KEY_1);
    }

    @Test
    public void when_value_existed_in_table_then_remove_returns_true() throws StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(set.remove(CELL_1), is(equalTo(true)));
    }

    @Test
    public void when_value_did_not_exist_in_table_then_remove_returns_false() throws StorageException {
        setAzureTableToContain();

        assertThat(set.remove(CELL_1), is(equalTo(false)));
    }

    @Test
    public void when_remove_null_then_false_returned() {
        assertThat(set.remove(null), is(equalTo(false)));
    }

    @Test
    public void when_object_to_be_removed_is_not_a_table_cell_then_remove_returns_false() {
        assertThat(set.remove(new Object()), is(equalTo(false)));
    }

    @Test
    public void when_contains_all_then_contains_all_returns_true() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        assertThat(set.containsAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(true)));
    }

    @Test
    public void when_does_not_contain_all_then_returns_false() throws StorageException {
        setAzureTableToContain(CELL_2);

        assertThat(set.containsAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(false)));
    }

    @Test(expected = NullPointerException.class)
    public void containAll_with_null_argument_not_allowed() {
        set.containsAll(null);
    }

    @Test
    public void add_all_adds_to_table() {
        set.addAll(Arrays.asList(CELL_1, CELL_2));

        verify(baseAzureTable).put(ROW_KEY_1, COLUMN_KEY_1, VALUE_1);
        verify(baseAzureTable).put(ROW_KEY_2, COLUMN_KEY_2, VALUE_2);
    }

    @Test(expected = NullPointerException.class)
    public void addAll_with_null_argument_not_allowed() {
        set.addAll(null);
    }

    @Test
    public void when_all_values_where_contained_then_return_false() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        assertThat(set.addAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(false)));
    }

    @Test
    public void when_some_value_was_not_present_then_return_true() throws StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(set.addAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(true)));
    }

    @Test
    public void remove_all_removes_from_table() {
        set.removeAll(Arrays.asList(CELL_1, CELL_2));

        verify(baseAzureTable).remove(ROW_KEY_1, COLUMN_KEY_1);
        verify(baseAzureTable).remove(ROW_KEY_2, COLUMN_KEY_2);
    }

    @Test(expected = NullPointerException.class)
    public void remove_all_with_null_argument_not_allowed() {
        set.removeAll(null);
    }

    @Test
    public void when_some_remove_all_values_where_contained_then_return_true() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        assertThat(set.removeAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(true)));
    }

    @Test
    public void when_non_of_remove_all_values_existed_then_return_false() throws StorageException {
        setAzureTableToContain();

        assertThat(set.removeAll(Arrays.asList(CELL_1, CELL_2)), is(equalTo(false)));
    }

    @Test
    public void clear_deletes_contained_entries() throws StorageException {
        setAzureTableToContain(CELL_1, CELL_2);

        set.clear();

        verify(baseAzureTable).remove(ROW_KEY_1, COLUMN_KEY_1);
        verify(baseAzureTable).remove(ROW_KEY_2, COLUMN_KEY_2);
    }

    //----------------------
    // Utilities
    //----------------------

    @SafeVarargs
    private final void setAzureTableToContain(Table.Cell<Bytes, Bytes, Bytes>... cells) throws StorageException {
        for (Table.Cell<Bytes, Bytes, Bytes> cell : cells) {
            when(baseAzureTable.get(cell.getRowKey(), cell.getColumnKey())).thenReturn(cell.getValue());
            when(baseAzureTable.contains(cell.getRowKey(), cell.getColumnKey())).thenReturn(true);
            when(baseAzureTable.put(eq(cell.getRowKey()), eq(cell.getColumnKey()), any(Bytes.class))).thenReturn(cell.getValue());
            when(baseAzureTable.remove(cell.getRowKey(), cell.getColumnKey())).thenReturn(cell.getValue());

        }
        AzureTestUtil.setAzureTableToContain(TABLE_NAME, azureTableRequestFactoryMock, azureTableCloudClientMock, cells);
    }


}
