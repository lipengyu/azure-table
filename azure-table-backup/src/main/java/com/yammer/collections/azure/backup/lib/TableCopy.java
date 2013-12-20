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
package com.yammer.collections.azure.backup.lib;

import com.google.common.collect.Table;

import static com.google.common.base.Preconditions.checkNotNull;

public class TableCopy<R, C, V> {

    public void perform(Table<R, C, V> sourceTable, Table<R, C, V> backupTable) throws TableCopyException {
        checkNotNull(sourceTable);
        checkNotNull(backupTable);
        try {
            for (Table.Cell<R, C, V> cell : sourceTable.cellSet()) {
                backupTable.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
        } catch (Exception e) {
            throw new TableCopyException(e);
        }
    }
}
