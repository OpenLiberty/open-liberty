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
package org.apache.myfaces.cdi.impl;

import java.util.Map;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.apache.myfaces.cdi.util.BeanProvider;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.cdi.view.ApplicationContextBean;
import org.apache.myfaces.cdi.view.ViewScopeBeanHolder;
import org.apache.myfaces.cdi.view.ViewScopeCDIMap;
import org.apache.myfaces.flow.cdi.FlowScopeBeanHolder;
import org.apache.myfaces.spi.ViewScopeProvider;

/**
 *
 * @author Leonardo Uribe
 */
public class CDIManagedBeanHandlerImpl extends ViewScopeProvider
{
    
    private BeanManager beanManager;
    
    private ViewScopeBeanHolder viewScopeBeanHolder;
    
    private FlowScopeBeanHolder flowScopeBeanHolder;
    
    public CDIManagedBeanHandlerImpl()
    {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        beanManager = CDIUtils.getBeanManager(externalContext);
        Object context = externalContext.getContext();
        if (context instanceof ServletContext)
        {
            ApplicationContextBean appBean = CDIUtils.lookup(beanManager, 
                ApplicationContextBean.class);
            appBean.setServletContext((ServletContext) context);
        }
    }
    
    private ViewScopeBeanHolder getViewScopeBeanHolder()
    {
        if (viewScopeBeanHolder == null)
        {
            viewScopeBeanHolder = BeanProvider.getContextualReference(
                beanManager, ViewScopeBeanHolder.class, false);
        }
        return viewScopeBeanHolder;
    }
    
    private FlowScopeBeanHolder getFlowScopeBeanHolder()
    {
        if (flowScopeBeanHolder == null)
        {
            flowScopeBeanHolder = BeanProvider.getContextualReference(
                beanManager, FlowScopeBeanHolder.class, false);
        }
        return flowScopeBeanHolder;
    }
    
    public Map<String, Object> createViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        return new ViewScopeCDIMap(facesContext, viewScopeId);
    }
    
    public Map<String, Object> restoreViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        return new ViewScopeCDIMap(facesContext, viewScopeId);
    }
    
    public String generateViewScopeId(FacesContext facesContext)
    {
        return getViewScopeBeanHolder().generateUniqueViewScopeId();
    }
    
    /**
     * 
     */
    public void onSessionDestroyed()
    {
        // In CDI case, the best way to deal with this is use a method 
        // with @PreDestroy annotation on a session scope bean 
        // ( ViewScopeBeanHolder.destroyBeans() ). There is no need
        // to do anything else in this location, but it is advised
        // in CDI the beans are destroyed at the end of the request,
        // not when invalidateSession() is called.
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            if (facesContext.getExternalContext().getSession(false) != null)
            {
                if (isViewScopeBeanHolderCreated(facesContext))
                {
                    getViewScopeBeanHolder().destroyBeans();                
                }
                if (isFlowScopeBeanHolderCreated(facesContext))
                {
                    getFlowScopeBeanHolder().destroyBeans();
                }
            }
        }
    }
    
    private boolean isViewScopeBeanHolderCreated(FacesContext facesContext)
    {
        return facesContext.getExternalContext().
            getSessionMap().containsKey(ViewScopeBeanHolder.VIEW_SCOPE_PREFIX_KEY);
    }
    
    
    private boolean isFlowScopeBeanHolderCreated(FacesContext facesContext)
    {
        return facesContext.getExternalContext().
            getSessionMap().containsKey(FlowScopeBeanHolder.FLOW_SCOPE_PREFIX_KEY);
    }

    @Override
    public void destroyViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        if (facesContext.getExternalContext().getSession(false) != null)
        {
            if (isViewScopeBeanHolderCreated(facesContext))
            {
                getViewScopeBeanHolder().destroyBeans(viewScopeId);
            }
        }
    }
}
