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
package org.apache.myfaces.flow.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.faces.context.FacesContext;

/**
 *
 * @author Leonardo Uribe
 */
class FlowScopeMap implements Map<Object,Object>
{
    private DefaultFacesFlowProvider _provider;
    
    private String _flowMapKey;
    
    private Map<Object,Object> _delegate;
        
    public FlowScopeMap(DefaultFacesFlowProvider provider, String flowMapKey)
    {
        this._provider = provider;
        this._flowMapKey = flowMapKey;
    }
    
    private Map<Object,Object> getWrapped(boolean create)
    {
        if (_delegate == null)
        {
            String fullToken = DefaultFacesFlowProvider.FLOW_SESSION_MAP_SUBKEY_PREFIX + 
                DefaultFacesFlowProvider.SEPARATOR_CHAR + _flowMapKey;
            FacesContext context = FacesContext.getCurrentInstance();
            _delegate = _provider.createOrRestoreMap(context, fullToken, create);
        }
        return _delegate;
    }

    public int size()
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return 0;
        }
        return map.size();
    }

    public boolean isEmpty()
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return false;
        }
        return map.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return false;
        }
        return map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return false;
        }
        return map.containsValue(value);
    }

    public Object get(Object key)
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return null;
        }
        return map.get(key);
    }

    public Object put(Object key, Object value)
    {
        return getWrapped(true).put(key, value);
    }

    public Object remove(Object key)
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return null;
        }
        return map.remove(key);
    }

    public void putAll(Map<? extends Object, ? extends Object> m)
    {
        getWrapped(true).putAll(m);
    }

    public void clear()
    {
        Map<Object,Object> map = getWrapped(false);
        if (map == null)
        {
            return;
        }
        map.clear();
    }

    public Set<Object> keySet()
    {
        return getWrapped(true).keySet();
    }

    public Collection<Object> values()
    {
        return getWrapped(true).values();
    }

    public Set<Entry<Object, Object>> entrySet()
    {
        return getWrapped(true).entrySet();
    }
    
}
