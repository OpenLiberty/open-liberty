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
package org.apache.myfaces.config.annotation;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.spi.impl.SpiUtils;



public abstract class LifecycleProviderFactory
{
    protected static final String FACTORY_DEFAULT = DefaultLifecycleProviderFactory.class.getName();

    private static final String FACTORY_KEY = LifecycleProviderFactory.class.getName();

    public static LifecycleProviderFactory getLifecycleProviderFactory()
    {
        // Since we always provide a StartupFacesContext on initialization time, this is safe:
        return getLifecycleProviderFactory(FacesContext.getCurrentInstance().getExternalContext());
    }
    
    public static LifecycleProviderFactory getLifecycleProviderFactory(ExternalContext ctx)
    {
        Map<String, Object> applicationMap = ctx.getApplicationMap();
        LifecycleProviderFactory instance = (LifecycleProviderFactory) applicationMap.get(FACTORY_KEY);
        if (instance != null)
        {
            return instance;
        }
        LifecycleProviderFactory lpf = null;
        try
        {

            if (System.getSecurityManager() != null)
            {
                final ExternalContext ectx = ctx; 
                lpf = (LifecycleProviderFactory)
                        AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>()
                        {
                            public Object run() throws PrivilegedActionException
                            {
                                return SpiUtils.build(ectx,
                                        LifecycleProviderFactory.class,
                                        FACTORY_DEFAULT);
                            }
                        });
            }
            else
            {
                lpf = (LifecycleProviderFactory) SpiUtils.build(ctx, LifecycleProviderFactory.class, FACTORY_DEFAULT);
            }
        }
        catch (PrivilegedActionException pae)
        {
            throw new FacesException(pae);
        }
        if (lpf != null)
        {
            applicationMap.put(FACTORY_KEY, lpf);
        }
        return lpf;
    }


    public static void setLifecycleProviderFactory(LifecycleProviderFactory instance)
    {
        FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(FACTORY_KEY, instance);
    }

    public abstract LifecycleProvider getLifecycleProvider(ExternalContext externalContext);

    public abstract void release();

}
