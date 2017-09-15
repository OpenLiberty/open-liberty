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

import org.apache.myfaces.cdi.util.CDIUtils;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.spi.BeanManager;
import org.apache.myfaces.cdi.util.BeanProvider;

/**
 * 
 *
 * @author Leonardo Uribe
 */
public class FlowScopeMap implements Map
{
    private BeanManager _beanManager;
    private String _currentClientWindowFlowId;
    private FlowScopeBeanHolder _flowScopeBeanHolder;
    private boolean _initOptional = false;
    
    public FlowScopeMap(BeanManager beanManager, String currentFlowMapKey)
    {
        this._beanManager = beanManager;
        this._currentClientWindowFlowId = currentFlowMapKey;
    }
    
    private FlowScopeBeanHolder getFlowScopeBeanHolder(boolean create)
    {
        if (_flowScopeBeanHolder == null)
        {
            if (create)
            {
                _flowScopeBeanHolder = CDIUtils.lookup(_beanManager,
                    FlowScopeBeanHolder.class);
            }
            else if (!_initOptional)
            {
                _flowScopeBeanHolder = BeanProvider.getContextualReference(
                    _beanManager, FlowScopeBeanHolder.class, true);
                _initOptional = true;
            }
        }
        return _flowScopeBeanHolder;
    }
    
    public int size()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return 0;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return 0;
        }
        return map.size();
    }
    
    public boolean isEmpty()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return true;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return true;
        }
        return map.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return false;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return false;
        }
        return map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return false;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return false;
        }
        return map.containsValue(value);
    }

    public Object get(Object key)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return null;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return null;
        }
        return map.get(key);
    }

    public Object put(Object key, Object value)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(true);
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(
            _beanManager, _currentClientWindowFlowId, true);
        return map.put(key, value);
    }

    public Object remove(Object key)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return null;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return null;
        }
        return map.remove(key);
    }

    public void putAll(Map m)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(true);
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(
            _beanManager, _currentClientWindowFlowId, true);
        map.putAll(m);
    }

    public void clear()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(false);
        if (flowScopeBeanHolder == null)
        {
            return;
        }
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(_beanManager,
            _currentClientWindowFlowId, false);
        if (map == null)
        {
            return;
        }
        map.clear();
    }

    public Set keySet()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(true);
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(
            _beanManager, _currentClientWindowFlowId, true);
        return map.keySet();
    }

    public Collection values()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(true);
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(
            _beanManager, _currentClientWindowFlowId, true);
        return map.values();
    }

    public Set entrySet()
    {
        FlowScopeBeanHolder flowScopeBeanHolder = getFlowScopeBeanHolder(true);
        Map<Object, Object> map = flowScopeBeanHolder.getFlowScopeMap(
            _beanManager, _currentClientWindowFlowId, true);
        return map.entrySet();
    }
}
