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
package com.yammer.collections.azure.serialization.json;

import com.google.common.base.Function;
import com.google.common.collect.Table;
import com.yammer.collections.transforming.TransformingTable;

public final class JsonSerializingTable {
    private JsonSerializingTable() {
    }

    public static <R, C, V> Table<R, C, V> create(Table<String, String, String> backingTable,
                                                  Class<R> rowClass, Class<C> columnClass, Class<V> valueClass) {
        Function<R, String> toRowFunction = new JsonSerializationFunction<>();
        Function<String, R> fromRowFunction = new JsonDeserializationFunction<>(rowClass);
        Function<C, String> toColumnFunction = new JsonSerializationFunction<>();
        Function<String, C> fromColumnFunction = new JsonDeserializationFunction<>(columnClass);
        Function<V, String> toValueFunction = new JsonSerializationFunction<>();
        Function<String, V> fromValueFunction = new JsonDeserializationFunction<>(valueClass);

        return TransformingTable.create(backingTable,
                toRowFunction, fromRowFunction,
                toColumnFunction, fromColumnFunction,
                toValueFunction, fromValueFunction
        );
    }


}
