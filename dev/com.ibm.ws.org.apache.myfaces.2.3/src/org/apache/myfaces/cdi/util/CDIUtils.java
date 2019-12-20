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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.ExternalContext;
import javax.faces.view.ViewScoped;
import org.apache.myfaces.webapp.AbstractFacesInitializer;

/**
 * Lookup code for Contextual Instances.
 */
public class CDIUtils
{
    public static BeanManager getBeanManager(ExternalContext externalContext)
    {
        return (BeanManager) externalContext.getApplicationMap().get(
            AbstractFacesInitializer.CDI_BEAN_MANAGER_INSTANCE);
    }



    public static <T> T lookup(BeanManager bm, Class<T> clazz)
    {
        Set<Bean<?>> beans = bm.getBeans(clazz);
        return resolveInstance(bm, beans, clazz);
    }

    public static Object lookup(BeanManager bm, String name)
    {
        Set<Bean<?>> beans = bm.getBeans(name);
        return resolveInstance(bm, beans, Object.class);
    }

    private static <T> T resolveInstance(BeanManager bm, Set<Bean<?>> beans, Type type)
    {
        Bean<?> bean = bm.resolve(beans);
        CreationalContext<?> cc = bm.createCreationalContext(bean);
        T dao = (T) bm.getReference(bean, type, cc);
        return dao;

    }
    
    @SuppressWarnings("unchecked")
    public static <T> Bean<T> resolve(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers)
    {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);

        for (Bean<?> bean : beans)
        {
            if (bean.getBeanClass() == beanClass)
            {
                return (Bean<T>) beanManager.resolve(Collections.<Bean<?>>singleton(bean));
            }
        }

        return (Bean<T>) beanManager.resolve(beans);
    }

    @SuppressWarnings("unchecked")
    public static <T> Bean<T> resolve(BeanManager beanManager, Type type, Annotation... qualifiers)
    {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);

        return (Bean<T>) beanManager.resolve(beans);
    }

    public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass, 
            boolean create, Annotation... qualifiers)
    {
        try
        {
            Bean<T> bean = resolve(beanManager, beanClass, qualifiers);
            return (bean != null) ? getInstance(beanManager, bean, create) : null;
        }
        catch (ContextNotActiveException e)
        {
            return null;
        }
    }

    public static <T> T getInstance(BeanManager beanManager, Type type, 
    boolean create, Annotation... qualifiers)
    {
        try
        {
            Bean<T> bean = resolve(beanManager, type, qualifiers);
            return (bean != null) ? getInstance(beanManager, bean, create) : null;
        }
        catch (ContextNotActiveException e)
        {
            return null;
        }
    }

    public static <T> T getInstance(BeanManager beanManager, Bean<T> bean, boolean create)
    {
        Context context = beanManager.getContext(bean.getScope());

        if (create)
        {
            return context.get(bean, beanManager.createCreationalContext(bean));
        }
        else
        {
            return context.get(bean);
        }
    }
    
    public static boolean isSessionScopeActive(BeanManager beanManager)
    {
        try 
        {
            Context ctx = beanManager.getContext(SessionScoped.class);
            return ctx != null;
        }
        catch (ContextNotActiveException ex)
        {
            //No op
        }
        catch (Exception ex)
        {
            // Sometimes on startup time, since there is no active request context, trying to grab session scope
            // throws NullPointerException.
            //No op
        }
        return false;
    }
    
    public static boolean isViewScopeActive(BeanManager beanManager)
    {
        try 
        {
            Context ctx = beanManager.getContext(ViewScoped.class);
            return ctx != null;
        }
        catch (ContextNotActiveException ex)
        {
            //No op
        }
        catch (Exception ex)
        {
            // Sometimes on startup time, since there is no active request context, trying to grab session scope
            // throws NullPointerException.
            //No op
        }
        return false;
    }
        
}