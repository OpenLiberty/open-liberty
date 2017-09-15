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

import java.lang.reflect.Type;
import java.util.Iterator;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.webapp.AbstractFacesInitializer;

/**
 * 
 * @author Leonardo Uribe
 */
public class CDIUtils
{
    public static BeanManager getBeanManager(ExternalContext externalContext)
    {
        return (BeanManager) externalContext.getApplicationMap().get(
            AbstractFacesInitializer.CDI_BEAN_MANAGER_INSTANCE);
    }

    /*
    public static BeanManager getBeanManagerFromJNDI()
    {
        try
        {
            // in an application server
            return (BeanManager) InitialContext.doLookup("java:comp/BeanManager");
        }
        catch (NamingException e)
        {
            // silently ignore
        }

        try
        {
            // in a servlet container
            return (BeanManager) InitialContext.doLookup("java:comp/env/BeanManager");
        }
        catch (NamingException e)
        {
            // silently ignore
        }
        return null;
    }*/

    @SuppressWarnings("unchecked")
    public static <T> T lookup(BeanManager bm, Class<T> clazz)
    {
        Iterator<Bean< ?>> iter = bm.getBeans(clazz).iterator();
        if (!iter.hasNext())
        {
            throw new IllegalStateException(
                "CDI BeanManager cannot find an instance of requested type " + 
                clazz.getName());
        }
        Bean<T> bean = (Bean<T>) iter.next();
        CreationalContext<T> ctx = bm.createCreationalContext(bean);
        T dao = (T) bm.getReference(bean, clazz, ctx);
        return dao;
    }

    @SuppressWarnings(
    {
        "unchecked", "rawtypes"
    })
    public static Object lookup(BeanManager bm, String name)
    {
        Iterator<Bean< ?>> iter = bm.getBeans(name).iterator();
        if (!iter.hasNext())
        {
            throw new IllegalStateException(
                "CDI BeanManager cannot find an instance of requested type '" + name + "'");
        }
        Bean bean = iter.next();
        CreationalContext ctx = bm.createCreationalContext(bean);
        Type type = (Type) bean.getTypes().iterator().next();
        return bm.getReference(bean, type, ctx);
    }
}