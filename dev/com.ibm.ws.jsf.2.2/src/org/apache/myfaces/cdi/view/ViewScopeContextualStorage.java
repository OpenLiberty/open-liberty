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


import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.Bean;
import org.apache.myfaces.cdi.util.ContextualInstanceInfo;

/**
 * This Storage holds all information needed for storing
 * View Scope instances in a context.
 * 
 * This scope requires passivation and is not concurrent.
 */
public class ViewScopeContextualStorage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final Map<Object, ContextualInstanceInfo<?>> contextualInstances;
    
    private final Map<String, Object> nameBeanKeyMap;
    
    private final BeanManager beanManager;
    
    private transient volatile boolean deactivated;

    /**
     * @param beanManager is needed for serialisation
     * @param concurrent whether the ContextualStorage might get accessed concurrently by different threads
     * @param passivationCapable whether the storage is for passivation capable Scopes
     */
    public ViewScopeContextualStorage(BeanManager beanManager)
    {
        this.beanManager = beanManager;
        contextualInstances = new HashMap<Object, ContextualInstanceInfo<?>>();
        nameBeanKeyMap = new HashMap<String, Object>();
        deactivated = false;
    }

    /**
     * @return the underlying storage map.
     */
    public Map<Object, ContextualInstanceInfo<?>> getStorage()
    {
        return contextualInstances;
    }
    
    public Map<String, Object> getNameBeanKeyMap()
    {
        return nameBeanKeyMap;
    }

    /**
     *
     * @param bean
     * @param creationalContext
     * @param <T>
     * @return
     */
    public <T> T createContextualInstance(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
        Object beanKey = getBeanKey(bean);

        // simply create the contextual instance
        ContextualInstanceInfo<T> instanceInfo = new ContextualInstanceInfo<T>();
        instanceInfo.setCreationalContext(creationalContext);
        instanceInfo.setContextualInstance(bean.create(creationalContext));

        contextualInstances.put(beanKey, instanceInfo);
        if(bean instanceof Bean)
        {
            String name = ((Bean<T>) bean).getName();
            if (name != null)
            {
                nameBeanKeyMap.put(name, beanKey);
            }
        }

        return instanceInfo.getContextualInstance();
    }

    /**
     * If the context is a passivating scope then we return
     * the passivationId of the Bean. Otherwise we use
     * the Bean directly.
     * @return the key to use in the context map
     */
    public <T> Object getBeanKey(Contextual<T> bean)
    {
        return ((PassivationCapable) bean).getId();
    }

    /**
     * Restores the Bean from its beanKey.
     * @see #getBeanKey(javax.enterprise.context.spi.Contextual)
     */
    public Contextual<?> getBean(Object beanKey)
    {
        return beanManager.getPassivationCapableBean((String) beanKey);
    }
    
    public boolean isActive()
    {
        return !deactivated;
    }
    
    public void deactivate()
    {
        deactivated = true;
    }
}
