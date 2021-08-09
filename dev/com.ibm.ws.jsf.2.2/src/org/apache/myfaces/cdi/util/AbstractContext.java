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

package org.apache.myfaces.cdi.util;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;
import java.util.Map;

/**
 * A skeleton containing the most important parts of a custom CDI Context.
 * An implementing Context needs to implement the missing methods from the
 * {@link Context} interface and {@link #getContextualStorage(boolean)}.
 * 
 * NOTE: Taken from Apache DeltaSpike
 */
public abstract class AbstractContext implements Context
{
    /**
     * Whether the Context is for a passivating scope.
     */
    private final boolean passivatingScope;

    protected AbstractContext(BeanManager beanManager)
    {
        passivatingScope = beanManager.isPassivatingScope(getScope());
    }

    /**
     * An implementation has to return the underlying storage which
     * contains the items held in the Context.
     * @param createIfNotExist whether a ContextualStorage shall get created if it doesn't yet exist.
     * @return the underlying storage
     */
    protected abstract ContextualStorage getContextualStorage(boolean createIfNotExist);

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
        checkActive();

        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return null;
        }

        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
        if (contextualInstanceInfo == null)
        {
            return null;
        }

        return (T) contextualInstanceInfo.getContextualInstance();
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
        checkActive();

        if (passivatingScope)
        {
            if (!(bean instanceof PassivationCapable))
            {
                throw new IllegalStateException(bean.toString() +
                        " doesn't implement " + PassivationCapable.class.getName());
            }
        }

        ContextualStorage storage = getContextualStorage(true);

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
        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return false;
        }
        ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage().get(storage.getBeanKey(bean));

        if (contextualInstanceInfo == null)
        {
            return false;
        }

        bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());

        return true;
    }

    /**
     * destroys all the Contextual Instances in the Storage returned by
     * {@link #getContextualStorage(boolean)}.
     */
    public void destroyAllActive()
    {
        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return;
        }

        destroyAllActive(storage);
    }

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
            Contextual bean = storage.getBean(entry.getKey());

            ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
            bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());
        }
    }

    /**
     * Make sure that the Context is really active.
     * @throws ContextNotActiveException if there is no active
     *         Context for the current Thread.
     */
    protected void checkActive()
    {
        if (!isActive())
        {
            throw new ContextNotActiveException("CDI context with scope annotation @"
                + getScope().getName() + " is not active with respect to the current thread");
        }
    }

}
