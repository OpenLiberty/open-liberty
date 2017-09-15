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
package org.apache.myfaces.shared.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;


public class HashMapUtils
{
    //~ Constructors -------------------------------------------------------------------------------

    protected HashMapUtils()
    {
        // block public access
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Calculates initial capacity needed to hold <code>size</code> elements in
     * a HashMap or Hashtable without forcing an expensive increase in internal
     * capacity. Capacity is based on the default load factor of .75.
     * <p>
     * Usage: <code>Map map = new HashMap(HashMapUtils.calcCapacity(10));<code>
     * </p>
     * @param size the number of items that will be put into a HashMap
     * @return initial capacity needed
     */
    public static final int calcCapacity(int size)
    {
        return ((size * 4) + 3) / 3;
    }

    /**
     * Creates a new <code>HashMap</code> that has all of the elements
     * of <code>map1</code> and <code>map2</code> (on key collision, the latter
     * override the former).
     *
     * @param map1 the fist hashmap to merge
     * @param map2 the second hashmap to merge
     * @return new hashmap
     */
    public static HashMap merge(Map map1, Map map2)
    {
        HashMap retval = new HashMap(calcCapacity(map1.size() + map2.size()));

        retval.putAll(map1);
        retval.putAll(map2);

        return retval;
    }

     /**
     * spit out each name/value pair
     */
    public static String mapToString(Map map)
    {
        Set entries = map.entrySet();
        Iterator iter = entries.iterator();
        StringBuffer buff = new StringBuffer();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            buff.append("[" + entry.getKey() + "," + entry.getValue() + "]\n");
        }
        return buff.toString();
    }

}
