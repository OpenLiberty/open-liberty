/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.message;

import java.util.HashMap;
import java.util.Map;

/**
 * A variation on HashMap which allows lookup by Class, via the string
 * returned by {@link Class#getName()}.
 */
public class StringMapImpl
                extends HashMap<String, Object>
                implements StringMap {

    private static final long serialVersionUID = -4590903451121887L;

    // Liberty defect - used to avoid resize
    public StringMapImpl(int isize, float factor) {
        super(isize, factor);
    }

    public StringMapImpl() {}

    public StringMapImpl(Map<String, Object> i) {
        super(i);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> key) {
        return (T) get(key.getName());
    }

    @Override
    public <T> void put(Class<T> key, T value) {
        put(key.getName(), value);
    }

    public <T> T remove(Class<T> key) {
        return key.cast(remove(key.getName()));
    }
}
