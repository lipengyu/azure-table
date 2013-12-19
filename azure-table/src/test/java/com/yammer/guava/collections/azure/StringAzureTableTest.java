package com.yammer.guava.collections.azure;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.microsoft.windowsazure.services.core.storage.utils.Base64;
import com.microsoft.windowsazure.services.table.client.TableOperation;
import com.microsoft.windowsazure.services.table.client.TableQuery;
import com.yammer.dropwizard.config.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

// TODO tidy up this test, its really difficult to read

@RunWith(MockitoJUnitRunner.class)
public class StringAzureTableTest {
    private static final String ROW_NAME = "row_name_fixed";
    private static final String EXISTING_COLUMN_KEY = "column_key_1";
    private static final String EXISTING_KEY_2 = "column_key_2";
    private static final String NON_EXISTENT_COLUMN_KEY = "non_existent_column_key";
    private static final String VALUE = "value1";
    private static final String NEW_VALUE = "value2";
    private static final String TABLE_NAME = "secretie_table";
    private static final String ENCODING = "UTF-8";
    @Mock
    private StringTableCloudClient stringTableCloudClientMock;
    @Mock
    private StringTableRequestFactory stringTableRequestFactoryMock;
    private StringAzureTable stringAzureTable;

    private String encode(String stringToBeEncoded) throws UnsupportedEncodingException {
        return Base64.encode(stringToBeEncoded.getBytes(ENCODING));

    }

    private String decode(String stringToBeDecoded) throws UnsupportedEncodingException {
        return new String(Base64.decode(stringToBeDecoded), ENCODING);
    }

    private StringEntity encodedStringEntity(String unEncodedRowName, String unEncodedColumnName, String unEncodedValue) throws UnsupportedEncodingException {
        return new StringEntity(encode(unEncodedRowName), encode(unEncodedColumnName), encode(unEncodedValue));
    }

    @Before
    public void setUp() throws IOException, ConfigurationException {
        stringAzureTable = new StringAzureTable(TABLE_NAME, stringTableCloudClientMock, stringTableRequestFactoryMock);
    }

    @Test
    public void when_columnKeySet_requested_then_all_keys_returned() throws UnsupportedEncodingException {
        // setup
        TableQuery<StringEntity> tableQuery = mock(TableQuery.class);
        when(stringTableRequestFactoryMock.selectAll(TABLE_NAME)).thenReturn(tableQuery);
        when(stringTableCloudClientMock.execute(tableQuery)).thenReturn(Arrays.asList(encodedStringEntity(ROW_NAME, EXISTING_COLUMN_KEY, VALUE),
                encodedStringEntity(ROW_NAME, EXISTING_KEY_2, VALUE)));

        // call under test
        Set<String> columnKeySet = stringAzureTable.columnKeySet();

        // assertions
        assertThat(columnKeySet, containsInAnyOrder(EXISTING_COLUMN_KEY, EXISTING_KEY_2));
    }

    @Test
    public void get_of_an_existing_value_returns_result_from_azure_table_returned() throws StorageException, UnsupportedEncodingException {
        // setup
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(ROW_NAME), encode(EXISTING_COLUMN_KEY))).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenReturn(encodedStringEntity(ROW_NAME, EXISTING_COLUMN_KEY, VALUE));

        // call under test
        String value = stringAzureTable.get(ROW_NAME, EXISTING_COLUMN_KEY);

        // assertions
        assertThat(value, is(equalTo(VALUE)));
    }

    @Test
    public void get_of_non_existing_entry_returns_null() {
        // setup
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(ROW_NAME, NON_EXISTENT_COLUMN_KEY)).thenReturn(retriveTableOperationMock);

        // call under test
        String value = stringAzureTable.get(ROW_NAME, NON_EXISTENT_COLUMN_KEY);

        // assertions
        assertThat(value, is(nullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_get_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        // setup
        StorageException storageExceptionMock = mock(StorageException.class);
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(ROW_NAME), encode(EXISTING_COLUMN_KEY))).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenThrow(storageExceptionMock);

        // call under test
        stringAzureTable.get(ROW_NAME, EXISTING_COLUMN_KEY);
    }

    @Test
    public void when_put_then_value_added_or_replaced_in_azure() throws StorageException, UnsupportedEncodingException {
        // setup
        TableOperation putTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.put(encode(ROW_NAME), encode(NON_EXISTENT_COLUMN_KEY), encode(NEW_VALUE))).thenReturn(putTableOperationMock);

        // call under test
        stringAzureTable.put(ROW_NAME, NON_EXISTENT_COLUMN_KEY, NEW_VALUE);

        // assert
        verify(stringTableCloudClientMock).execute(TABLE_NAME, putTableOperationMock);
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_put_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        // setup
        StorageException storageExceptionMock = mock(StorageException.class);
        TableOperation putTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.put(encode(ROW_NAME), encode(EXISTING_COLUMN_KEY), encode(VALUE))).thenReturn(putTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, putTableOperationMock)).thenThrow(storageExceptionMock);

        // call under test
        stringAzureTable.put(ROW_NAME, EXISTING_COLUMN_KEY, VALUE);
    }

    @Test
    public void when_delete_then_deleted_in_azure() throws StorageException, UnsupportedEncodingException {
        // setup
        StringEntity result = encodedStringEntity(ROW_NAME, EXISTING_COLUMN_KEY, VALUE);
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(ROW_NAME), encode(EXISTING_COLUMN_KEY))).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenReturn(result);

        TableOperation deleteTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.delete(result)).thenReturn(deleteTableOperationMock);


        // call under test
        stringAzureTable.remove(ROW_NAME, EXISTING_COLUMN_KEY);

        // assertions
        verify(stringTableCloudClientMock).execute(TABLE_NAME, deleteTableOperationMock);
    }

    @Test
    public void when_key_does_not_exist_then_delete_return_null() throws StorageException {
        // setup
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(ROW_NAME, NON_EXISTENT_COLUMN_KEY)).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenReturn(null);

        // call under test
        stringAzureTable.remove(ROW_NAME, NON_EXISTENT_COLUMN_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_delete_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        // internal get
        StringEntity result = encodedStringEntity(ROW_NAME, EXISTING_COLUMN_KEY, VALUE);
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(ROW_NAME), encode(EXISTING_COLUMN_KEY))).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenReturn(result);
        // delete
        TableOperation deleteTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.delete(result)).thenReturn(deleteTableOperationMock);
        // delete error
        StorageException storageExceptionMock = mock(StorageException.class);
        when(stringTableCloudClientMock.execute(TABLE_NAME, deleteTableOperationMock)).thenThrow(storageExceptionMock);

        stringAzureTable.remove(ROW_NAME, EXISTING_COLUMN_KEY);
    }

    @Test
    public void cellSet_returns_all_table_cells() throws UnsupportedEncodingException {
        // setup
        TableQuery<StringEntity> tableQuery = mock(TableQuery.class);
        when(stringTableRequestFactoryMock.selectAll(TABLE_NAME)).thenReturn(tableQuery);
        when(stringTableCloudClientMock.execute(tableQuery)).thenReturn(Arrays.asList(encodedStringEntity(ROW_NAME, EXISTING_COLUMN_KEY, VALUE), encodedStringEntity(ROW_NAME,
                EXISTING_KEY_2, VALUE)));

        // call under test
        Set<Table.Cell<String, String, String>> cellSet = stringAzureTable.cellSet();

        // assertions
        final Table.Cell<String, String, String> expectedCell1 = Tables.immutableCell(ROW_NAME, EXISTING_COLUMN_KEY, VALUE);
        final Table.Cell<String, String, String> expectedCell2 = Tables.immutableCell(ROW_NAME, EXISTING_KEY_2, VALUE);
        assertThat(cellSet, containsInAnyOrder(expectedCell1, expectedCell2));
    }
}