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
package org.apache.myfaces.view;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.PreDestroyViewMapEvent;
import org.apache.myfaces.spi.ViewScopeProvider;
import org.apache.myfaces.spi.ViewScopeProviderFactory;

/**
 * This wrapper has these objectives:
 * 
 * - Isolate the part that needs to be saved with the view (viewScopeId) from
 *   the part that should remain into session (bean map). This class will be
 *   serialized when UIViewRoot.saveState() is called.
 * - Decouple the way how the view scope map is stored. For example, in 
 *   CDI view scope a session scope bean is used, and in default view scope
 *   the same session map is used but using a prefix.
 *
 * @author Leonardo Uribe
 */
public class ViewScopeProxyMap implements Map<String, Object>, StateHolder
{
    private String _viewScopeId;
    
    private transient Map<String, Object> _delegate;

    public ViewScopeProxyMap()
    {
    }
    
    
    public String getViewScopeId()
    {
        return _viewScopeId;
    }
    
    public void forceCreateWrappedMap(FacesContext facesContext)
    {
        getWrapped();
    }
    
    public Map<String, Object> getWrapped()
    {
        if (_delegate == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            
            ViewScopeProviderFactory factory = ViewScopeProviderFactory.getViewScopeHandlerFactory(
                facesContext.getExternalContext());
            
            ViewScopeProvider handler = factory.getViewScopeHandler(facesContext.getExternalContext());
            
            if (_viewScopeId == null)
            {
                _viewScopeId = handler.generateViewScopeId(facesContext);
                _delegate = handler.createViewScopeMap(facesContext, _viewScopeId);
            }
            else
            {
                _delegate = handler.restoreViewScopeMap(facesContext, _viewScopeId);
            }
        }
        return _delegate;
    }
    
    public int size()
    {
        return getWrapped().size();
    }

    public boolean isEmpty()
    {
        return getWrapped().isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return getWrapped().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return getWrapped().containsValue(value);
    }

    public Object get(Object key)
    {
        return getWrapped().get(key);
    }

    public Object put(String key, Object value)
    {
        return getWrapped().put(key, value);
    }

    public Object remove(Object key)
    {
        return getWrapped().remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> m)
    {
        getWrapped().putAll(m);
    }

    public void clear()
    {
        /*
         * The returned Map must be implemented such that calling clear() on the Map causes
         * Application.publishEvent(java.lang.Class, java.lang.Object) to be called, passing
         * ViewMapDestroyedEvent.class as the first argument and this UIViewRoot instance as the second argument.
         */
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getApplication().publishEvent(facesContext, 
                PreDestroyViewMapEvent.class, facesContext.getViewRoot());

        getWrapped().clear();
    }

    public Set<String> keySet()
    {
        return getWrapped().keySet();
    }

    public Collection<Object> values()
    {
        return getWrapped().values();
    }

    public Set<Entry<String, Object>> entrySet()
    {
        return getWrapped().entrySet();
    }

    public void restoreState(FacesContext context, Object state)
    {
        _viewScopeId = (String) state;
    }

    public Object saveState(FacesContext context)
    {
        return _viewScopeId;
    }

    public boolean isTransient()
    {
        return false;
    }

    public void setTransient(boolean newTransientValue)
    {
    }

}
