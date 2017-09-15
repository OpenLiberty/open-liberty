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
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;

/**
 *
 */
class ClientWindowFacesFlowLRUMap extends LRUMap implements Serializable
{
    private transient FlowScopeBeanHolder holder;

    public ClientWindowFacesFlowLRUMap()
    {
    }

    public ClientWindowFacesFlowLRUMap(int maxSize)
    {
        super(maxSize);
    }

    public ClientWindowFacesFlowLRUMap(int maxSize, boolean scanUntilRemovable)
    {
        super(maxSize, scanUntilRemovable);
    }

    public ClientWindowFacesFlowLRUMap(int maxSize, float loadFactor)
    {
        super(maxSize, loadFactor);
    }

    public ClientWindowFacesFlowLRUMap(int maxSize, float loadFactor, boolean scanUntilRemovable)
    {
        super(maxSize, loadFactor, scanUntilRemovable);
    }

    public ClientWindowFacesFlowLRUMap(Map map)
    {
        super(map);
    }

    public ClientWindowFacesFlowLRUMap(Map map, boolean scanUntilRemovable)
    {
        super(map, scanUntilRemovable);
    }
    
    public void setFlowScopeBeanHolder(FlowScopeBeanHolder holder)
    {
        this.holder = holder;
    }
    
    @Override
    protected boolean removeLRU(LinkEntry entry)
    {
        if (holder != null)
        {
            holder.clearFlowMap((String) entry.getKey());
        }
        return super.removeLRU(entry);
    }
}
