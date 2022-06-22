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
package org.apache.myfaces.cdi.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.FacesContext;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;

/**
 *
 * @author Leonardo Uribe
 */
public class ViewScopeCDIMap implements Map<String, Object>
{
    private String _viewScopeId;
    
    private ViewScopeContextualStorage storage;

    public ViewScopeCDIMap(FacesContext facesContext)
    {
        BeanManager beanManager = CDIUtils.
            getBeanManager(facesContext.getExternalContext());

        ViewScopeBeanHolder bean = CDIUtils.lookup(
            beanManager, ViewScopeBeanHolder.class);

        // 1. get a new view scope id
        _viewScopeId = bean.generateUniqueViewScopeId();

        storage = bean.
            getContextualStorage(beanManager, _viewScopeId);
    }
    
    public ViewScopeCDIMap(FacesContext facesContext, String viewScopeId)
    {
        BeanManager beanManager = CDIUtils.
            getBeanManager(facesContext.getExternalContext());

        ViewScopeBeanHolder bean = CDIUtils.lookup(
            beanManager, ViewScopeBeanHolder.class);

        // 1. get a new view scope id
        _viewScopeId = viewScopeId;

        storage = bean.
            getContextualStorage(beanManager, _viewScopeId);
    }
    
    private ViewScopeContextualStorage getStorage()
    {
        if (storage != null && !storage.isActive())
        {
            storage = null;
        }
        if (storage == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            BeanManager beanManager = CDIUtils.
                getBeanManager(facesContext.getExternalContext());

            ViewScopeBeanHolder bean = CDIUtils.lookup(
                beanManager, ViewScopeBeanHolder.class);
            
            storage = bean.
                getContextualStorage(beanManager, _viewScopeId);
        }
        return storage;
    }
    
    private Map<String, Object> getNameBeanKeyMap()
    {
        return getStorage().getNameBeanKeyMap();
    }
    
    private Map<Object, ContextualInstanceInfo<?>> getCreationalContextInstances()
    {
        return getStorage().getStorage();
    }
    
    public String getViewScopeId()
    {
        return _viewScopeId;
    }
    
    public int size()
    {
        return this.getNameBeanKeyMap().size();
    }

    public boolean isEmpty()
    {
        return this.getNameBeanKeyMap().isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return this.getNameBeanKeyMap().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        if (value != null)
        {
            for (Map.Entry<Object, ContextualInstanceInfo<?>> entry : 
                getCreationalContextInstances().entrySet())
            {
                if (entry.getValue() != null &&
                    value.equals(entry.getValue().getContextualInstance()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public Object get(Object key)
    {
        Object beanKey = this.getNameBeanKeyMap().get(key);
        if (beanKey != null)
        {
            ContextualInstanceInfo<?> info = this.getCreationalContextInstances().get(beanKey);
            return info == null ? null : info.getContextualInstance();
        }
        return null;
    }

    public Object put(String key, Object value)
    {
        Object beanKey = new _ContextualKey(key);
        this.getNameBeanKeyMap().put(key, beanKey);
        ContextualInstanceInfo info = new ContextualInstanceInfo();
        info.setContextualInstance(value);
        return this.getCreationalContextInstances().put(beanKey, info);
    }

    public Object remove(Object key)
    {
        Object beanKey = this.getNameBeanKeyMap().remove(key);
        ContextualInstanceInfo info = this.getCreationalContextInstances().remove(beanKey);
        return info == null ? null : info.getContextualInstance();
    }

    public void putAll(Map<? extends String, ? extends Object> m)
    {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        boolean destroyed = false;
        // If the scope was already destroyed through an invalidateSession(), the storage instance
        // that is holding this map could be obsolete, so we need to grab the right instance from
        // the bean holder.
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            BeanManager beanManager = CDIUtils.
                getBeanManager(facesContext.getExternalContext());

            if (beanManager != null)
            {
                ViewScopeBeanHolder bean = CDIUtils.lookup(
                    beanManager, ViewScopeBeanHolder.class);
                if (bean != null)
                {
                    ViewScopeContextualStorage st = bean.
                        getContextualStorage(beanManager, _viewScopeId);
                    if (st != null)
                    {
                        ViewScopeContextImpl.destroyAllActive(st);
                        storage = null;
                        destroyed = true;
                        bean.removeStorage(_viewScopeId);
                    }
                }
            }
        }
        if (!destroyed)
        {
            ViewScopeContextImpl.destroyAllActive(storage);
        }
    }

    public Set<String> keySet()
    {
        return this.getNameBeanKeyMap().keySet();
    }

    public Collection<Object> values()
    {
        List<Object> values = new ArrayList<Object>(this.getNameBeanKeyMap().size());
        for (Map.Entry<String, Object> entry : 
                this.getNameBeanKeyMap().entrySet())
        {
            if (entry.getValue() != null)
            {
                ContextualInstanceInfo info = 
                    this.getCreationalContextInstances().get(entry.getValue());
                if (info != null)
                {
                    values.add(info.getContextualInstance());
                }
            }
        }
        return values;
    }

    public Set<Entry<String, Object>> entrySet()
    {
        Set<Entry<String, Object>> values = new HashSet<Entry<String, Object>>();
        for (Map.Entry<String, Object> entry : 
                this.getNameBeanKeyMap().entrySet())
        {
            if (entry.getValue() != null)
            {
                ContextualInstanceInfo info = 
                    this.getCreationalContextInstances().get(entry.getValue());
                if (info != null)
                {
                    values.add(new EntryWrapper(entry));
                }
            }
        }
        return values;
    }
    
    private class EntryWrapper<String, Object> implements Entry<String, Object>
    {
        private Map.Entry<String, Object> entry;
        
        public EntryWrapper(Map.Entry<String, Object> entry)
        {
            this.entry = entry;
        }

        public String getKey()
        {
            return entry.getKey();
        }

        public Object getValue()
        {
            ContextualInstanceInfo<?> info = getCreationalContextInstances().get(entry.getValue());
            return (Object) (info == null ? null : info.getContextualInstance());
        }

        public Object setValue(Object value)
        {
            ContextualInstanceInfo info = getCreationalContextInstances().get(entry.getValue());
            Object oldValue = null;
            if (info != null)
            {
                info.setContextualInstance(value);
            }
            else
            {
                info = new ContextualInstanceInfo();
                info.setContextualInstance(value);
                getCreationalContextInstances().put(entry.getValue(), info);
            }
            return oldValue;
        }
    }
}
