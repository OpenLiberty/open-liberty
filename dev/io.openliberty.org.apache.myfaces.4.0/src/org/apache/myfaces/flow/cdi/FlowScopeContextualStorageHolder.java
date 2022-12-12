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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.context.FacesContext;
import jakarta.faces.flow.Flow;
import jakarta.faces.flow.FlowHandler;
import jakarta.faces.lifecycle.ClientWindow;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;
import org.apache.myfaces.cdi.util.ContextualStorage;
import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.cdi.util.AbstractContextualStorageHolder;
import org.apache.myfaces.flow.FlowUtils;
import org.apache.myfaces.util.lang.LRULinkedHashMap;

/**
 * This holder will store the flow scope active ids and it's beans for the current
 * HTTP Session. We use standard SessionScoped bean to not need
 * to treat async-supported and similar headache.
 * 
 * @author lu4242
 */
@Typed(FlowScopeContextualStorageHolder.class)
@SessionScoped
public class FlowScopeContextualStorageHolder 
        extends AbstractContextualStorageHolder<ContextualStorage>
        implements Serializable
{
    /**
     * key: client window id + flow id
     * value: the {@link ContextualStorage} which holds all the
     * {@link jakarta.enterprise.inject.spi.Bean}s.
     */
    
    // clientwindow <> List<Flow>
    private Map<String, List<String>> activeFlowMapKeys;
    
    private LRULinkedHashMap<String, String> clientWindowExpirationStack;
    
    public static final String CURRENT_FLOW_SCOPE_MAP = "oam.CURRENT_FLOW_SCOPE_MAP";

    public FlowScopeContextualStorageHolder()
    {
    }
    
    @PostConstruct
    @Override
    public void init()
    {
        super.init();
        
        activeFlowMapKeys = new ConcurrentHashMap<>();

        FacesContext facesContext = FacesContext.getCurrentInstance();

        Integer numberOfClientWindowsInSession =
                MyfacesConfig.getCurrentInstance(facesContext).getNumberOfClientWindows();
        clientWindowExpirationStack = new LRULinkedHashMap<>(numberOfClientWindowsInSession, (eldest) ->
        {
            clearFlowMap(FacesContext.getCurrentInstance(), eldest.getKey());
        });

        refreshClientWindow(facesContext);
    }

    public Map<Object, Object> getFlowScopeMap(BeanManager beanManager, String flowClientWindowId, boolean create)
    {
        ContextualStorage contextualStorage = getContextualStorage(flowClientWindowId, create);
        if (contextualStorage == null)
        {
            return null;
        }

        ContextualInstanceInfo info = contextualStorage.getStorage().get(CURRENT_FLOW_SCOPE_MAP);
        if (info == null && create)
        {
            info = new ContextualInstanceInfo<>();
            contextualStorage.getStorage().put(CURRENT_FLOW_SCOPE_MAP, info);
        }
        if (info == null)
        {
            return null;
        }

        Map<Object, Object> map = (Map<Object, Object>) info.getContextualInstance();
        if (map == null && create)
        {
            map = new HashMap<>();
            info.setContextualInstance(map);
        }
        return map;
    }

    public void refreshClientWindow(FacesContext facesContext)
    {
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        if (cw != null && cw.getId() != null)
        {
            synchronized (clientWindowExpirationStack)
            {
                clientWindowExpirationStack.remove(cw.getId());
                clientWindowExpirationStack.put(cw.getId(), "");
            }
        }
    }
    
    public void clearFlowMap(FacesContext facesContext, String clientWindowId)
    {
        List<String> activeFlowKeys = activeFlowMapKeys.remove(clientWindowId);
        if (activeFlowKeys != null && !activeFlowKeys.isEmpty())
        {
            for (String flowMapKey : activeFlowKeys)
            {
                ContextualStorage contextualStorage = storageMap.remove(flowMapKey);
                if (contextualStorage != null)
                {
                    destroyAll(contextualStorage, facesContext);
                }
            }
        }
    }
    
    public List<String> getActiveFlowMapKeys(FacesContext facesContext)
    {
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        String baseKey = cw.getId();
        List<String> activeFlowKeys = activeFlowMapKeys.get(baseKey);
        if (activeFlowKeys == null)
        {
            return Collections.emptyList();
        }

        return activeFlowKeys;
    }
    
    public void createCurrentFlowScope(FacesContext facesContext)
    {
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        String baseKey = cw.getId();
        
        FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
        Flow flow = flowHandler.getCurrentFlow(facesContext);
        String flowMapKey = FlowUtils.getFlowMapKey(facesContext, flow);

        List<String> activeFlowKeys = activeFlowMapKeys.computeIfAbsent(baseKey, k -> new ArrayList<>());
        activeFlowKeys.add(0, flowMapKey);
        activeFlowMapKeys.put(baseKey, activeFlowKeys);
        refreshClientWindow(facesContext);
    }
    
    public void destroyCurrentFlowScope(FacesContext facesContext)
    {
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        String baseKey = cw.getId();
        
        FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
        Flow flow = flowHandler.getCurrentFlow(facesContext);
        String flowMapKey = FlowUtils.getFlowMapKey(facesContext, flow);

        ContextualStorage contextualStorage = storageMap.remove(flowMapKey);
        if (contextualStorage != null)
        {
            destroyAll(contextualStorage, facesContext);
        }
        
        List<String> activeFlowKeys = activeFlowMapKeys.get(baseKey);
        if (activeFlowKeys != null && !activeFlowKeys.isEmpty())
        {
            activeFlowKeys.remove(flowMapKey);
        }
    }

    @Override
    protected boolean isSkipDestroy(Map.Entry<Object, ContextualInstanceInfo<?>> entry)
    {
        return CURRENT_FLOW_SCOPE_MAP.equals(entry.getKey());
    }

    @Override
    protected ContextualStorage newContextualStorage(String slotId)
    {
        return new ContextualStorage(beanManager, true);
    }

    public static FlowScopeContextualStorageHolder getInstance(FacesContext facesContext)
    {
        return getInstance(facesContext, false);
    }
    
    public static FlowScopeContextualStorageHolder getInstance(FacesContext facesContext, boolean create)
    {
        return getInstance(facesContext, FlowScopeContextualStorageHolder.class, create);
    }
}
