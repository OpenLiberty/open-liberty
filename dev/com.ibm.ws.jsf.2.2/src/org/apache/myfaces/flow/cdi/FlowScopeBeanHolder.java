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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import javax.faces.flow.FlowHandler;
import javax.faces.lifecycle.ClientWindow;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;
import org.apache.myfaces.cdi.util.ContextualStorage;
import org.apache.myfaces.cdi.view.ApplicationContextBean;
import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.flow.FlowReference;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.context.ExceptionHandlerImpl;


/**
 *
 * This holder will store the flow scope active ids and it's beans for the current
 * HTTP Session. We use standard SessionScoped bean to not need
 * to treat async-supported and similar headache.
 * 
 * @author lu4242
 */
@SessionScoped
public class FlowScopeBeanHolder implements Serializable
{
    /**
     * key: client window id + flow id
     * value: the {@link ContextualStorage} which holds all the
     * {@link javax.enterprise.inject.spi.Bean}s.
     */
    private Map<String, ContextualStorage> storageMap = new ConcurrentHashMap<String, ContextualStorage>();
    
    private Map<String, List<String>> activeFlowMapKeys = new ConcurrentHashMap<String, List<String>>();
    
    private FacesFlowClientWindowCollection windowCollection = null;
    
    public static final String CURRENT_FLOW_SCOPE_MAP = "oam.CURRENT_FLOW_SCOPE_MAP";
    
    private static final String FLOW_SCOPE_PREFIX = "oam.flow.SCOPE";
    
    public static final String FLOW_SCOPE_PREFIX_KEY = FLOW_SCOPE_PREFIX+".KEY";
    
    @Inject
    ApplicationContextBean applicationContextBean;

    public FlowScopeBeanHolder()
    {
    }
    
    @PostConstruct
    public void init()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        this.refreshClientWindow(facesContext);
        facesContext.getExternalContext().getSessionMap().put(FLOW_SCOPE_PREFIX_KEY,
            1);
    }
    
    /**
     * This method will return the ContextualStorage or create a new one
     * if no one is yet assigned to the current flowClientWindowId.
     * @param beanManager we need the CDI {@link BeanManager} for serialisation.
     * @param flowClientWindowId the flowClientWindowId for the current flow.
     */
    public ContextualStorage getContextualStorage(BeanManager beanManager, String flowClientWindowId)
    {
        ContextualStorage contextualStorage = storageMap.get(flowClientWindowId);
        if (contextualStorage == null)
        {
            synchronized (this)
            {
                contextualStorage = storageMap.get(flowClientWindowId);
                if (contextualStorage == null)
                {
                    contextualStorage = new ContextualStorage(beanManager, true, true);
                    storageMap.put(flowClientWindowId, contextualStorage);
                }
            }
        }

        return contextualStorage;
    }
    
    public ContextualStorage getContextualStorageNoCreate(BeanManager beanManager, String flowClientWindowId)
    {
        return storageMap.get(flowClientWindowId);
    }

    public Map<String, ContextualStorage> getStorageMap()
    {
        return storageMap;
    }
    
    public Map<Object, Object> getFlowScopeMap(
        BeanManager beanManager, String flowClientWindowId, boolean create)
    {
        Map<Object, Object> map = null;
        if (create)
        {
            ContextualStorage contextualStorage = getContextualStorage(
                beanManager, flowClientWindowId);
            ContextualInstanceInfo info = contextualStorage.getStorage().get(CURRENT_FLOW_SCOPE_MAP);
            if (info == null)
            {
                info = new ContextualInstanceInfo<Object>();
                contextualStorage.getStorage().put(CURRENT_FLOW_SCOPE_MAP, info);
            }
            map = (Map<Object, Object>) info.getContextualInstance();
            if (map == null)
            {
                map = new HashMap<Object,Object>();
                info.setContextualInstance(map);
            }
        }
        else
        {
            ContextualStorage contextualStorage = getContextualStorageNoCreate(
                beanManager, flowClientWindowId);
            if (contextualStorage != null)
            {
                ContextualInstanceInfo info = contextualStorage.getStorage().get(CURRENT_FLOW_SCOPE_MAP);
                if (info != null)
                {
                    map = (Map<Object, Object>) info.getContextualInstance();
                }
            }
        }
        return map;
    }

    /**
     *
     * This method will replace the storageMap and with
     * a new empty one.
     * This method can be used to properly destroy the WindowBeanHolder beans
     * without having to sync heavily. Any
     * {@link javax.enterprise.inject.spi.Bean#destroy(Object, javax.enterprise.context.spi.CreationalContext)}
     * should be performed on the returned old storage map.
     * @return the old storageMap.
     */
    public Map<String, ContextualStorage> forceNewStorage()
    {
        Map<String, ContextualStorage> oldStorageMap = storageMap;
        storageMap = new ConcurrentHashMap<String, ContextualStorage>();
        return oldStorageMap;
    }

    /**
     * This method properly destroys all current &#064;WindowScoped beans
     * of the active session and also prepares the storage for new beans.
     * It will automatically get called when the session context closes
     * but can also get invoked manually, e.g. if a user likes to get rid
     * of all it's &#064;WindowScoped beans.
     */
    //@PreDestroy
    public void destroyBeans()
    {
        // we replace the old windowBeanHolder beans with a new storage Map
        // an afterwards destroy the old Beans without having to care about any syncs.
        Map<String, ContextualStorage> oldWindowContextStorages = forceNewStorage();

        for (ContextualStorage contextualStorage : oldWindowContextStorages.values())
        {
            FlowScopedContextImpl.destroyAllActive(contextualStorage);
        }
    }
    
    /**
     * See description on ViewScopeBeanHolder for details about how this works
     */
    @PreDestroy
    public void destroyBeansOnPreDestroy()
    {
        Map<String, ContextualStorage> oldWindowContextStorages = forceNewStorage();
        if (!oldWindowContextStorages.isEmpty())
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ServletContext servletContext = null;
            if (facesContext == null)
            {
                try
                {
                    servletContext = applicationContextBean.getServletContext();
                }
                catch (Throwable e)
                {
                    Logger.getLogger(FlowScopeBeanHolder.class.getName()).log(Level.WARNING,
                        "Cannot locate servletContext to create FacesContext on @PreDestroy flow scope beans. "
                                + "The beans will be destroyed without active FacesContext instance.");
                    servletContext = null;
                }
            }
            if (facesContext == null &&
                servletContext != null)
            {
                try
                {
                    ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, false);
                    ExceptionHandler exceptionHandler = new ExceptionHandlerImpl();
                    facesContext = new StartupFacesContextImpl(externalContext, 
                            (ReleaseableExternalContext) externalContext, exceptionHandler, false);
                    for (ContextualStorage contextualStorage : oldWindowContextStorages.values())
                    {
                        FlowScopedContextImpl.destroyAllActive(contextualStorage);
                    }
                }
                finally
                {
                    facesContext.release();
                }
            }
            else
            {
                for (ContextualStorage contextualStorage : oldWindowContextStorages.values())
                {
                    FlowScopedContextImpl.destroyAllActive(contextualStorage);
                }
            }
        }
    }
    
    public void refreshClientWindow(FacesContext facesContext)
    {
        if (windowCollection == null)
        {
            Integer ft = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).
                    getNumberOfFacesFlowClientWindowIdsInSession();
            windowCollection = new FacesFlowClientWindowCollection(new ClientWindowFacesFlowLRUMap(ft));
        }
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        if (cw != null && cw.getId() != null)
        {
            windowCollection.setFlowScopeBeanHolder(this);
            windowCollection.put(cw.getId(), "");
        }
    }
    
    public void clearFlowMap(String clientWindowId)
    {
        List<String> activeFlowKeys = activeFlowMapKeys.remove(clientWindowId);
        if (activeFlowKeys != null && !activeFlowKeys.isEmpty())
        {
            for (String flowMapKey : activeFlowKeys)
            {
                ContextualStorage contextualStorage = storageMap.remove(flowMapKey);
                if (contextualStorage != null)
                {
                    FlowScopedContextImpl.destroyAllActive(contextualStorage);
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
        else
        {
            return activeFlowKeys;
        }
    }
    
    public String getFlowMapKey(FacesContext facesContext, FlowReference flowReference)
    {
        Flow flow = null;
        if (flowReference.getDocumentId() == null)
        {
            flow = facesContext.getApplication().getFlowHandler().getFlow(
                facesContext, "", flowReference.getId());
        }
        else
        {
            flow = facesContext.getApplication().getFlowHandler().getFlow(
                facesContext, flowReference.getDocumentId(), flowReference.getId());
        }
        if (flow != null)
        {
            return flow.getClientWindowFlowId(facesContext.getExternalContext().getClientWindow());
        }
        return null;
    }

    public void createCurrentFlowScope(FacesContext facesContext)
    {
        ClientWindow cw = facesContext.getExternalContext().getClientWindow();
        String baseKey = cw.getId();
        
        FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
        Flow flow = flowHandler.getCurrentFlow(facesContext);
        String flowMapKey = flow.getClientWindowFlowId(
            facesContext.getExternalContext().getClientWindow());

        List<String> activeFlowKeys = activeFlowMapKeys.get(baseKey);
        if (activeFlowKeys == null)
        {
            activeFlowKeys = new ArrayList<String>();
            
        }
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
        String flowMapKey = flow.getClientWindowFlowId(
            facesContext.getExternalContext().getClientWindow());

        ContextualStorage contextualStorage = storageMap.remove(flowMapKey);
        if (contextualStorage != null)
        {
            FlowScopedContextImpl.destroyAllActive(contextualStorage);
        }
        
        List<String> activeFlowKeys = activeFlowMapKeys.get(baseKey);
        if (activeFlowKeys != null && !activeFlowKeys.isEmpty())
        {
            activeFlowKeys.remove(flowMapKey);
        }
    }
}
