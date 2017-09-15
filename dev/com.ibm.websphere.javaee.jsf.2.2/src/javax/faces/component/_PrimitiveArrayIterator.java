/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * This utility iterator uses reflection to iterate over primitive arrays.
 */
class _PrimitiveArrayIterator implements Iterator<Object>
{
    public _PrimitiveArrayIterator(Object primitiveArray)
    {
        if (primitiveArray == null ||
            !primitiveArray.getClass().isArray())
        {
            throw new IllegalArgumentException("Requires a primitive array");
        }

        _primitiveArray = primitiveArray;
        _size = Array.getLength(primitiveArray);
    }

    public boolean hasNext()
    {
        return (_position < _size);
    }

    public Object next()
    {
        return Array.get(_primitiveArray, _position++);
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private Object _primitiveArray;
    private int    _size;
    private int    _position;
}
