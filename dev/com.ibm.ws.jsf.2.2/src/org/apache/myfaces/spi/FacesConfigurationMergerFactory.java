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

import org.apache.myfaces.spi.impl.DefaultFacesConfigurationMergerFactory;
import org.apache.myfaces.spi.impl.SpiUtils;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 * SPI to provide a FacesConfigurationMergerFactory implementation and thus
 * a custom FacesConfigurationMerger instance.
 *
 * @author Jakob Korherr
 * @since 2.0.3
 */
public abstract class FacesConfigurationMergerFactory
{

    protected static final String FACTORY_DEFAULT = DefaultFacesConfigurationMergerFactory.class.getName();

    private static final String FACTORY_KEY = FacesConfigurationMergerFactory.class.getName();

    public static FacesConfigurationMergerFactory getFacesConfigurationMergerFactory(ExternalContext ctx)
    {
        FacesConfigurationMergerFactory factory
                = (FacesConfigurationMergerFactory) ctx.getApplicationMap().get(FACTORY_KEY);
        
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
                factory = (FacesConfigurationMergerFactory) AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<Object>()
                        {
                            public Object run() throws PrivilegedActionException
                            {
                                return SpiUtils.build(ectx,
                                        FacesConfigurationMergerFactory.class,
                                        FACTORY_DEFAULT);
                            }
                        });
            }
            else
            {
                factory = (FacesConfigurationMergerFactory) SpiUtils
                        .build(ctx, FacesConfigurationMergerFactory.class, FACTORY_DEFAULT);
            }
        }
        catch (PrivilegedActionException pae)
        {
            throw new FacesException(pae);
        }

        if (factory != null)
        {
            // cache instance on ApplicationMap
            setFacesConfigurationMergerFactory(ctx, factory);
        }

        return factory;
    }

    public static void setFacesConfigurationMergerFactory(ExternalContext ctx,
                                                          FacesConfigurationMergerFactory factory)
    {
        ctx.getApplicationMap().put(FACTORY_KEY, factory);
    }

    public abstract FacesConfigurationMerger getFacesConfigurationMerger(ExternalContext externalContext);

}
