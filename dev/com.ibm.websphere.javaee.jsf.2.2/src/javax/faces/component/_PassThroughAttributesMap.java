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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class _PassThroughAttributesMap implements Map<String, Object>, Serializable
{
    private static final long serialVersionUID = -9106932179394257866L;
    
    private UIComponentBase _component;
    
    _PassThroughAttributesMap(UIComponentBase component)
    {
        _component = component;
    }

    
    /**
     * Return the map containing the attributes.
     * <p/>
     * This method is package-scope so that the UIComponentBase class can access it
     * directly when serializing the component.
     */
    Map<String, Object> getUnderlyingMap()
    {
        StateHelper stateHelper = _component.getStateHelper(false);
        Map<String, Object> attributes = null;
        if (stateHelper != null)
        {
            attributes = (Map<String, Object>) stateHelper.get(UIComponentBase.PropertyKeys.passThroughAttributesMap);
        }
        return attributes == null ? Collections.EMPTY_MAP : attributes;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return getUnderlyingMap().equals(obj);
    }

    @Override
    public int hashCode()
    {
        return getUnderlyingMap().hashCode();
    }

    public int size()
    {
        return getUnderlyingMap().size();
    }

    public boolean isEmpty()
    {
        return getUnderlyingMap().isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return getUnderlyingMap().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return getUnderlyingMap().containsValue(value);
    }

    public Object get(Object key)
    {
        return getUnderlyingMap().get(key);
    }

    public Object put(String key, Object value)
    {
        return _component.getStateHelper().put(
            UIComponentBase.PropertyKeys.passThroughAttributesMap, key, value);
    }

    public Object remove(Object key)
    {
        return _component.getStateHelper().remove(
            UIComponentBase.PropertyKeys.passThroughAttributesMap, key);
    }

    /**
     * Call put(key, value) for each entry in the provided map.
     */
    public void putAll(Map<? extends String, ?> t)
    {
        for (Map.Entry<? extends String, ?> entry : t.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Return a set of all <i>attributes</i>. Properties of the underlying
     * UIComponent are not included, nor value-bindings.
     */
    public Set<Map.Entry<String, Object>> entrySet()
    {
        return getUnderlyingMap().entrySet();
    }

    public Set<String> keySet()
    {
        return getUnderlyingMap().keySet();
    }

    public void clear()
    {
        getUnderlyingMap().clear();
    }

    public Collection<Object> values()
    {
        return getUnderlyingMap().values();
    }
}
