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
package org.apache.myfaces.flow.cdi;

import java.io.Serializable;

/**
 * This class is a wrapper used to deal with concurrency issues when accessing the inner LRUMap.
 */
class FacesFlowClientWindowCollection implements Serializable
{
    private ClientWindowFacesFlowLRUMap lruMap;

    public FacesFlowClientWindowCollection(ClientWindowFacesFlowLRUMap lruMap)
    {
        this.lruMap = lruMap;
    }

    public FacesFlowClientWindowCollection()
    {
    }
    
    public synchronized void put(String key, String value)
    {
        lruMap.put(key, value);
    }
    
    public synchronized String get(String key)
    {
        return (String) lruMap.get(key);
    }
    
    public synchronized void remove(String key)
    {
        lruMap.remove(key);
    }
    
    public synchronized boolean isEmpty()
    {
        return lruMap.isEmpty();
    }
    
    public void setFlowScopeBeanHolder(FlowScopeBeanHolder holder)
    {
        lruMap.setFlowScopeBeanHolder(holder);
    }
    
}
