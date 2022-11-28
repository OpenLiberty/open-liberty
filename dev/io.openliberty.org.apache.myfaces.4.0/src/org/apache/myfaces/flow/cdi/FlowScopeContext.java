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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.faces.context.FacesContext;
import jakarta.faces.flow.Flow;
import jakarta.faces.flow.FlowHandler;
import jakarta.faces.flow.FlowScoped;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;
import org.apache.myfaces.cdi.util.ContextualStorage;
import org.apache.myfaces.flow.FlowReference;
import org.apache.myfaces.flow.FlowUtils;

/**
 * Implementation of FlowScope.
 * 
 * @author Leonardo Uribe
 */
@Typed()
public class FlowScopeContext implements Context
{
    /**
     * Whether the Context is for a passivating scope.
     */
    private final boolean passivatingScope;

    /**
     * needed for serialisation and passivationId
     */
    private BeanManager beanManager;
    
    private Map<Class, FlowReference> flowBeanReferences;

    public FlowScopeContext(BeanManager beanManager, 
        Map<Class, FlowReference> flowBeanReferences)
    {
        this.beanManager = beanManager;
        this.flowBeanReferences = flowBeanReferences;
        this.passivatingScope = beanManager.isPassivatingScope(getScope());
    }

    protected FlowScopeContextualStorageHolder getStorageHolder(FacesContext facesContext)
    {
        return FlowScopeContextualStorageHolder.getInstance(facesContext, true);
    }
    
    public String getCurrentClientWindowFlowId(FacesContext facesContext)
    {
        String flowMapKey = null;
        FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
        Flow flow = flowHandler.getCurrentFlow(facesContext);
        if (flow != null)
        {
            flowMapKey = FlowUtils.getFlowMapKey(facesContext, flow);
        }
        return flowMapKey;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return FlowScoped.class;
    }

    @Override
    public boolean isActive()
    {
        return isActive(FacesContext.getCurrentInstance());
    }

    public boolean isActive(FacesContext facesContext)
    {
        if (facesContext == null)
        {
            return false;
        }
        
        Flow flow = facesContext.getApplication().getFlowHandler().getCurrentFlow(facesContext);
        return flow != null;
    }

    protected void checkActive(FacesContext facesContext)
    {
        if (!isActive(facesContext))
        {
            throw new ContextNotActiveException("CDI context with scope annotation @"
                + getScope().getName() + " is not active with respect to the current thread");
        }
    }

    public boolean isPassivatingScope()
    {
        return passivatingScope;
    }

    @Override
    public <T> T get(Contextual<T> bean)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        checkActive(facesContext);

        FlowReference reference = flowBeanReferences.get(((Bean) bean).getBeanClass());
        if (reference != null)
        {
            String flowMapKey = FlowUtils.getFlowMapKey(facesContext, reference);
            if (flowMapKey != null)
            {
                ContextualStorage storage = getContextualStorage(facesContext, false, flowMapKey);
                if (storage != null)
                {
                    Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
                    ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

                    if (contextualInstanceInfo != null)
                    {
                        return (T) contextualInstanceInfo.getContextualInstance();
                    }
                }
            }
            else
            {
                throw new IllegalStateException("Flow "+ reference.getId()+
                    " cannot be found when resolving bean " +bean.toString());
            }
        }
        
        List<String> activeFlowMapKeys = getStorageHolder(facesContext).getActiveFlowMapKeys(facesContext);
        for (String flowMapKey : activeFlowMapKeys)
        {
            ContextualStorage storage = getContextualStorage(facesContext, false, flowMapKey);
            if (storage == null)
            {
                continue;
            }

            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
            if (contextualInstanceInfo == null)
            {
                continue;
            }

            return (T) contextualInstanceInfo.getContextualInstance();
        }
        return null;
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        checkActive(facesContext);

        if (passivatingScope && !(bean instanceof PassivationCapable))
        {
            throw new IllegalStateException(bean.toString() +
                    " doesn't implement " + PassivationCapable.class.getName());
        }
        
        FlowReference reference = flowBeanReferences.get(((Bean) bean).getBeanClass());
        if (reference != null)
        {
            String flowMapKey = FlowUtils.getFlowMapKey(facesContext, reference);
            if (flowMapKey == null)
            {
                throw new IllegalStateException("Flow " + reference.getId()+
                    " cannot be found when resolving bean " + bean.toString());
            }

            ContextualStorage storage = getContextualStorage(facesContext, false, flowMapKey);
            if (storage != null)
            {
                Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
                ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

                if (contextualInstanceInfo != null)
                {
                    return (T) contextualInstanceInfo.getContextualInstance();
                }
            }

            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
            // Since it is possible to have only the flow id without documentId, the best
            // is first get the flow using flowHandler.getFlow and then check if the flow is
            // active or not, but using the documentId and id of the retrieved flow.
            Flow flow = flowHandler.getFlow(facesContext, 
                reference.getDocumentId() == null ? "" : reference.getDocumentId(), reference.getId());
            if (flow == null)
            {
                throw new IllegalStateException(bean.toString() + "cannot be created because flow "
                        + reference.getId() + " is not registered");
            }
            if (!flowHandler.isActive(facesContext, flow.getDefiningDocumentId(), flow.getId())) 
            {
                throw new IllegalStateException(bean.toString() + "cannot be created if flow "
                        + reference.getId() + " is not active");
            }

            storage = getContextualStorage(facesContext, true, flowMapKey); // now create it
            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
            if (contextualInstanceInfo != null)
            {
                @SuppressWarnings("unchecked")
                final T instance = (T) contextualInstanceInfo.getContextualInstance();
                if (instance != null)
                {
                    return instance;
                }
            }

            return storage.createContextualInstance(bean, creationalContext);
        }

        List<String> activeFlowMapKeys = getStorageHolder(facesContext).getActiveFlowMapKeys(facesContext);
        for (String flowMapKey : activeFlowMapKeys)
        {
            ContextualStorage storage = getContextualStorage(facesContext, false, flowMapKey);

            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
            if (contextualInstanceInfo != null)
            {
                @SuppressWarnings("unchecked")
                final T instance = (T) contextualInstanceInfo.getContextualInstance();
                if (instance != null)
                {
                    return instance;
                }
            }
        }
        
        ContextualStorage storage = getContextualStorage(facesContext, true,
                getCurrentClientWindowFlowId(facesContext));
        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
        if (contextualInstanceInfo != null)
        {
            @SuppressWarnings("unchecked")
            final T instance = (T) contextualInstanceInfo.getContextualInstance();
            if (instance != null)
            {
                return instance;
            }
        }

        return storage.createContextualInstance(bean, creationalContext);
    }

    protected ContextualStorage getContextualStorage(FacesContext context, boolean createIfNotExist,
            String clientWindowFlowId)
    {
        if (clientWindowFlowId == null)
        {
            throw new ContextNotActiveException(this.getClass().getName() + ": no current active flow");
        }

        return getStorageHolder(context).getContextualStorage(clientWindowFlowId, createIfNotExist);
    }

    public static void destroyAll(FacesContext facesContext)
    {
        FlowScopeContextualStorageHolder manager = FlowScopeContextualStorageHolder.getInstance(facesContext);
        if (manager != null)
        {
            manager.destroyAll(facesContext);
        }
    }
}
