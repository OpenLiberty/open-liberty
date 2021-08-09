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
package org.apache.myfaces.spi;

import java.security.AccessController;
import java.security.PrivilegedActionException;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;

import org.apache.myfaces.spi.impl.DefaultFacesConfigurationProviderFactory;
import org.apache.myfaces.spi.impl.SpiUtils;

/**
 * SPI to provide a FacesConfigurationProviderFactory implementation and thus
 * a custom FacesConfigurationProvider instance.
 *
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public abstract class FacesConfigurationProviderFactory
{

    protected static final String FACTORY_DEFAULT = DefaultFacesConfigurationProviderFactory.class.getName();

    private static final String FACTORY_KEY = FacesConfigurationProviderFactory.class.getName();

    public static FacesConfigurationProviderFactory getFacesConfigurationProviderFactory(ExternalContext ctx)
    {
        FacesConfigurationProviderFactory factory
                = (FacesConfigurationProviderFactory) ctx.getApplicationMap().get(FACTORY_KEY);
        if (factory != null)
        {
            // use cached instance
            return factory;
        }

        // create new instance from service entry
        try
        {

            if (System.getSecurityManager() != null)
            {
                final ExternalContext ectx = ctx;
                factory = (FacesConfigurationProviderFactory) AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<Object>()
                        {
                            public Object run() throws PrivilegedActionException
                            {
                                //return DiscoverSingleton.find(
                                return SpiUtils.build(ectx,
                                        FacesConfigurationProviderFactory.class,
                                        FACTORY_DEFAULT);
                            }
                        });
            }
            else
            {
                factory = (FacesConfigurationProviderFactory)
                        SpiUtils.build(ctx, FacesConfigurationProviderFactory.class, FACTORY_DEFAULT);
            }
        }
        catch (PrivilegedActionException pae)
        {
            throw new FacesException(pae);
        }

        if (factory != null)
        {
            // cache instance on ApplicationMap
            setFacesConfigurationProviderFactory(ctx, factory);
        }

        return factory;
    }

    public static void setFacesConfigurationProviderFactory(ExternalContext ctx,
                                                            FacesConfigurationProviderFactory factory)
    {
        ctx.getApplicationMap().put(FACTORY_KEY, factory);
    }

    public abstract FacesConfigurationProvider getFacesConfigurationProvider(ExternalContext externalContext);

}
