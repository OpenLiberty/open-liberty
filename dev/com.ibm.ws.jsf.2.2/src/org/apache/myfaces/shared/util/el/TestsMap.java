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
package org.apache.myfaces.shared.util.el;


/**
 * You can use this class to perform tests.
 * Use this for example in JSF backing beans, like BaseFace.getUserInRole(String).
 */
public abstract class TestsMap extends GenericMap
{

    /**
     * This method should return the result of the test.
     */
    public abstract boolean getTest(String testKey);

    protected Object getValue(Object testKey)
    {
        return Boolean.valueOf(getTest( (String) testKey ));
    }

    public Object get(Object key)
    {
        if( ! (key instanceof String) )
        {
            return null;
        }
        return Boolean.valueOf(getTest( (String)key ));
    }

    public Boolean put(String key, Boolean value)
    {
        return Boolean.FALSE;
    }
}


