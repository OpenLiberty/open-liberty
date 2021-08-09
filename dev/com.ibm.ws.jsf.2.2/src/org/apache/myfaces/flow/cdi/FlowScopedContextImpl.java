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
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import javax.faces.flow.FlowHandler;
import javax.faces.flow.FlowScoped;
import org.apache.myfaces.cdi.util.BeanProvider;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;
import org.apache.myfaces.cdi.util.ContextualStorage;
import org.apache.myfaces.flow.FlowReference;
import org.apache.myfaces.flow.util.FlowUtils;

/**
 * Minimal implementation of FlowScope.
 * 
 * @TODO: We need something better for this part. The problem is the beans
 * are just put under the session map using the old know SubKeyMap hack used
 * in Flash object, but CDI implementations like OWB solves the passivation
 * problem better. The ideal is provide a myfaces specific SPI interface, to
 * allow provide custom implementation of this detail.
 * 
 * @TODO: FlowHandler.transition() method should call this object when the
 * user enter or exit a flow.
 *
 * @author Leonardo Uribe
 */
@Typed()
public class FlowScopedContextImpl implements Context
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

    //private FlowScopeBeanHolder flowScopeBeanHolder;
    
    public FlowScopedContextImpl(BeanManager beanManager, 
        Map<Class, FlowReference> flowBeanReferences)
    {
        this.beanManager = beanManager;
        this.flowBeanReferences = flowBeanReferences;
        passivatingScope = beanManager.isPassivatingScope(getScope());
    }
    
    /*
    public void initFlowContext(FlowScopeBeanHolder flowScopeBeanHolder)
    {
        this.flowScopeBeanHolder = flowScopeBeanHolder;
    }*/
    
    protected FlowScopeBeanHolder getFlowScopeBeanHolder()
    {
        return getFlowScopeBeanHolder(FacesContext.getCurrentInstance());
    }
    
    protected FlowScopeBeanHolder getFlowScopeBeanHolder(FacesContext facesContext)
    {
        FlowScopeBeanHolder flowScopeBeanHolder = (FlowScopeBeanHolder) 
            facesContext.getExternalContext().getApplicationMap().get(
                "oam.flow.FlowScopeBeanHolder");
        if (flowScopeBeanHolder == null)
        {
            flowScopeBeanHolder = BeanProvider.getContextualReference(
                beanManager, FlowScopeBeanHolder.class, false);
            facesContext.getExternalContext().getApplicationMap().put(
                "oam.flow.FlowScopeBeanHolder", flowScopeBeanHolder);
        }
        return flowScopeBeanHolder;
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

    /**
     * An implementation has to return the underlying storage which
     * contains the items held in the Context.
     * @param createIfNotExist whether a ContextualStorage shall get created if it doesn't yet exist.
     * @return the underlying storage
     */
    protected ContextualStorage getContextualStorage(boolean createIfNotExist, String clientWindowFlowId)
    {
        //FacesContext facesContext = FacesContext.getCurrentInstance();
        //String clientWindowFlowId = getCurrentClientWindowFlowId(facesContext);
        if (clientWindowFlowId == null)
        {
            throw new ContextNotActiveException("FlowScopedContextImpl: no current active flow");
        }

        if (createIfNotExist)
        {
            return getFlowScopeBeanHolder().getContextualStorage(beanManager, clientWindowFlowId);
        }
        else
        {
            return getFlowScopeBeanHolder().getContextualStorageNoCreate(beanManager, clientWindowFlowId);
        }
    }

    public Class<? extends Annotation> getScope()
    {
        return FlowScoped.class;
    }

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
        Flow flow = facesContext.getApplication().
            getFlowHandler().getCurrentFlow(facesContext);
        
        return flow != null;
    }

    /**
     * @return whether the served scope is a passivating scope
     */
    public boolean isPassivatingScope()
    {
        return passivatingScope;
    }

    @Override
    public <T> T get(Contextual<T> bean)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        checkActive(facesContext);

        
        FlowReference reference = flowBeanReferences.get(((Bean)bean).getBeanClass());
        if (reference != null)
        {
            String flowMapKey = FlowUtils.getFlowMapKey(facesContext, reference);
            if (flowMapKey != null)
            {
                ContextualStorage storage = getContextualStorage(false, flowMapKey);
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
        
        List<String> activeFlowMapKeys = getFlowScopeBeanHolder().getActiveFlowMapKeys(facesContext);
        for (String flowMapKey : activeFlowMapKeys)
        {
            ContextualStorage storage = getContextualStorage(false, flowMapKey);
            if (storage == null)
            {
                //return null;
                continue;
            }

            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
            if (contextualInstanceInfo == null)
            {
                //return null;
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
        
        FlowReference reference = flowBeanReferences.get(((Bean)bean).getBeanClass());
        if (reference != null)
        {
            String flowMapKey = FlowUtils.getFlowMapKey(facesContext, reference);
            if (flowMapKey != null)
            {
                ContextualStorage storage = getContextualStorage(false, flowMapKey);
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
                    " cannot be found when resolving bean " + bean.toString());
            }
            
            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
            // Since it is possible to have only the flow id without documentId, the best
            // is first get the flow using flowHandler.getFlow and then check if the flow is
            // active or not, but using the documentId and id of the retrieved flow.
            Flow flow = flowHandler.getFlow(facesContext, 
                reference.getDocumentId() == null ? "" : reference.getDocumentId(), reference.getId());
            if (flow == null)
            {
                throw new IllegalStateException(bean.toString() + "cannot be created because flow "+ reference.getId()+
                    " is not registered");
            }
            if (!flowHandler.isActive(facesContext, flow.getDefiningDocumentId(), flow.getId())) 
            {
                throw new IllegalStateException(bean.toString() + "cannot be created if flow "+ reference.getId()+
                    " is not active");
            }
            
            ContextualStorage storage = getContextualStorage(true, flowMapKey);
            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

            if (contextualInstanceInfo != null)
            {
                @SuppressWarnings("unchecked")
                final T instance =  (T) contextualInstanceInfo.getContextualInstance();

                if (instance != null)
                {
                    return instance;
                }
            }

            return storage.createContextualInstance(bean, creationalContext);
        }

        List<String> activeFlowMapKeys = getFlowScopeBeanHolder().getActiveFlowMapKeys(facesContext);
        for (String flowMapKey : activeFlowMapKeys)
        {
            ContextualStorage storage = getContextualStorage(false, flowMapKey);

            Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
            ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

            if (contextualInstanceInfo != null)
            {
                @SuppressWarnings("unchecked")
                final T instance =  (T) contextualInstanceInfo.getContextualInstance();

                if (instance != null)
                {
                    return instance;
                }
            }

        }
        
        ContextualStorage storage = getContextualStorage(true, getCurrentClientWindowFlowId(facesContext));
        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

        if (contextualInstanceInfo != null)
        {
            @SuppressWarnings("unchecked")
            final T instance =  (T) contextualInstanceInfo.getContextualInstance();

            if (instance != null)
            {
                return instance;
            }
        }
            
        return storage.createContextualInstance(bean, creationalContext);
    }

    /**
     * Destroy the Contextual Instance of the given Bean.
     * @param bean dictates which bean shall get cleaned up
     * @return <code>true</code> if the bean was destroyed, <code>false</code> if there was no such bean.
     */
    public boolean destroy(Contextual bean)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        List<String> activeFlowMapKeys = getFlowScopeBeanHolder().getActiveFlowMapKeys(facesContext);
        for (String flowMapKey : activeFlowMapKeys)
        {
            ContextualStorage storage = getContextualStorage(false, flowMapKey);
            if (storage == null)
            {
                //return false;
                continue;
            }
            ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage().get(storage.getBeanKey(bean));

            if (contextualInstanceInfo == null)
            {
                //return false;
                continue;
            }

            bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());
            return true;
        }
        return false;
    }

    /**
     * destroys all the Contextual Instances in the Storage returned by
     * {@link #getContextualStorage(boolean)}.
     */
    /*
    public void destroyAllActive()
    {
        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return;
        }

        destroyAllActive(storage);
    }*/

    /**
     * Destroys all the Contextual Instances in the specified ContextualStorage.
     * This is a static method to allow various holder objects to cleanup
     * properly in &#064;PreDestroy.
     */
    public static void destroyAllActive(ContextualStorage storage)
    {
        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        for (Map.Entry<Object, ContextualInstanceInfo<?>> entry : contextMap.entrySet())
        {
            if (!FlowScopeBeanHolder.CURRENT_FLOW_SCOPE_MAP.equals(entry.getKey()))
            {
                Contextual bean = storage.getBean(entry.getKey());

                ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
                bean.destroy(contextualInstanceInfo.getContextualInstance(), 
                    contextualInstanceInfo.getCreationalContext());
            }
        }
    }

    /**
     * Make sure that the Context is really active.
     * @throws ContextNotActiveException if there is no active
     *         Context for the current Thread.
     */
    protected void checkActive(FacesContext facesContext)
    {
        if (!isActive(facesContext))
        {
            throw new ContextNotActiveException("CDI context with scope annotation @"
                + getScope().getName() + " is not active with respect to the current thread");
        }
    }

}
