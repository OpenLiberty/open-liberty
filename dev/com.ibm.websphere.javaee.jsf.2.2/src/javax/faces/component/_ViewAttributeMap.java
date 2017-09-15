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
import java.util.Map;
import java.util.Set;


class _ViewAttributeMap implements Map<String, Object>, Serializable
{
    private static final long serialVersionUID = -9106832109394257866L;

    private static final String RESET_SAVE_STATE_MODE_KEY = 
            "oam.view.resetSaveStateMode";

    /**
     * Key under UIViewRoot to generated unique ids for components added 
     * by @ResourceDependency effect.
     */
    private static final String RESOURCE_DEPENDENCY_UNIQUE_ID_KEY =
              "oam.view.resourceDependencyUniqueId";
    private static final String UNIQUE_ID_COUNTER_KEY =
              "oam.view.uniqueIdCounter";
    
    private Map<String, Object> _delegate;
    private UIViewRoot _root;

    public _ViewAttributeMap(UIViewRoot root, Map<String, Object> delegate)
    {
        this._delegate = delegate;
        this._root = root;
    }

    public int size()
    {
        return _delegate.size();
    }

    public boolean isEmpty()
    {
        return _delegate.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return _delegate.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return _delegate.containsValue(value);
    }

    public Object get(Object key)
    {
        checkKey(key);
        int keyLength = ((String)key).length();

        if (RESET_SAVE_STATE_MODE_KEY.length() == keyLength
            && RESET_SAVE_STATE_MODE_KEY.equals(key))
        {
            return _root.getResetSaveStateMode();
        }
        if (RESOURCE_DEPENDENCY_UNIQUE_ID_KEY.length() == keyLength
            && RESOURCE_DEPENDENCY_UNIQUE_ID_KEY.equals(key))
        {
            return _root.isResourceDependencyUniqueId();
        }
        if (UNIQUE_ID_COUNTER_KEY.length() == keyLength
            && UNIQUE_ID_COUNTER_KEY.equals(key))
        {
            return _root.getStateHelper().get(UIViewRoot.PropertyKeys.uniqueIdCounter);
        }
        return _delegate.get(key);
    }

    public Object put(String key, Object value)
    {
        int keyLength = ((String)key).length();

        if (RESET_SAVE_STATE_MODE_KEY.length() == keyLength
            && RESET_SAVE_STATE_MODE_KEY.equals(key))
        {
            Integer b = _root.getResetSaveStateMode();
            _root.setResetSaveStateMode(value == null ? 0 : (Integer) value);
            return b;
        }
        if (RESOURCE_DEPENDENCY_UNIQUE_ID_KEY.length() == keyLength
            && RESOURCE_DEPENDENCY_UNIQUE_ID_KEY.equals(key))
        {
            boolean b = _root.isResourceDependencyUniqueId();
            _root.setResourceDependencyUniqueId(value == null ? false : (Boolean) value);
            return b;
        }
        if (UNIQUE_ID_COUNTER_KEY.length() == keyLength
            && UNIQUE_ID_COUNTER_KEY.equals(key))
        {
            Integer v = (Integer) _root.getStateHelper().get(UIViewRoot.PropertyKeys.uniqueIdCounter);
            _root.getStateHelper().put(UIViewRoot.PropertyKeys.uniqueIdCounter, value);
            return v;
        }
        return _delegate.put(key, value);
    }

    public Object remove(Object key)
    {
        return _delegate.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> m)
    {
        _delegate.putAll(m);
    }

    public void clear()
    {
        _delegate.clear();
    }

    public Set<String> keySet()
    {
        return _delegate.keySet();
    }

    public Collection<Object> values()
    {
        return _delegate.values();
    }

    public Set<Entry<String, Object>> entrySet()
    {
        return _delegate.entrySet();
    }

    public boolean equals(Object o)
    {
        return _delegate.equals(o);
    }

    public int hashCode()
    {
        return _delegate.hashCode();
    }

    public String toString()
    {
        return _delegate.toString();
    }
    
    private void checkKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        if (!(key instanceof String))
        {
            throw new ClassCastException("key is not a String");
        }
    }
}
