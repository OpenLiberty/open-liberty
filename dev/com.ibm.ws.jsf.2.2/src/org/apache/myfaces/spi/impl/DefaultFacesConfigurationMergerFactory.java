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
package org.apache.myfaces.spi.impl;

import org.apache.myfaces.config.DefaultFacesConfigurationMerger;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.FacesConfigurationMerger;
import org.apache.myfaces.spi.FacesConfigurationMergerFactory;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jakob Korherr
 */
public class DefaultFacesConfigurationMergerFactory extends FacesConfigurationMergerFactory
{

    public static final String FACES_CONFIGURATION_MERGER = FacesConfigurationMerger.class.getName();
    public static final String FACES_CONFIGURATION_MERGER_INSTANCE_KEY = FACES_CONFIGURATION_MERGER + ".INSTANCE";

    @Override
    public FacesConfigurationMerger getFacesConfigurationMerger(ExternalContext externalContext)
    {
        // check for cached instance
        FacesConfigurationMerger returnValue = (FacesConfigurationMerger)
                externalContext.getApplicationMap().get(FACES_CONFIGURATION_MERGER_INSTANCE_KEY);

        if (returnValue == null)
        {
            final ExternalContext extContext = externalContext;
            try
            {
                if (System.getSecurityManager() != null)
                {
                    returnValue = AccessController.doPrivileged(
                            new java.security.PrivilegedExceptionAction<FacesConfigurationMerger>()
                            {
                                public FacesConfigurationMerger run() throws ClassNotFoundException,
                                        NoClassDefFoundError,
                                        InstantiationException,
                                        IllegalAccessException,
                                        InvocationTargetException,
                                        PrivilegedActionException
                                {
                                    return resolveFacesConfigurationMergerFromService(extContext);
                                }
                            });
                }
                else
                {
                    returnValue = resolveFacesConfigurationMergerFromService(extContext);
                }

                // cache the result on the ApplicationMap
                externalContext.getApplicationMap().put(FACES_CONFIGURATION_MERGER_INSTANCE_KEY, returnValue);
            }
            catch (ClassNotFoundException e)
            {
                // ignore
            }
            catch (NoClassDefFoundError e)
            {
                // ignore
            }
            catch (InstantiationException e)
            {
                getLogger().log(Level.SEVERE, "", e);
            }
            catch (IllegalAccessException e)
            {
                getLogger().log(Level.SEVERE, "", e);
            }
            catch (InvocationTargetException e)
            {
                getLogger().log(Level.SEVERE, "", e);
            }
            catch (PrivilegedActionException e)
            {
                throw new FacesException(e);
            }
        }


        return returnValue;
    }

    private FacesConfigurationMerger resolveFacesConfigurationMergerFromService(ExternalContext externalContext)
            throws ClassNotFoundException,
                   NoClassDefFoundError,
                   InstantiationException,
                   IllegalAccessException,
                   InvocationTargetException,
                   PrivilegedActionException
    {
        // get all fitting SPI implementations (no need to cache this since it's only called once per JSF-app)
        List<String> classList = ServiceProviderFinderFactory.getServiceProviderFinder(externalContext)
                .getServiceProviderList(FACES_CONFIGURATION_MERGER);

        // create the instance using decorator pattern
        return ClassUtils.buildApplicationObject(FacesConfigurationMerger.class,
                classList, new DefaultFacesConfigurationMerger());
    }

    private Logger getLogger()
    {
        return Logger.getLogger(DefaultFacesConfigurationMergerFactory.class.getName());
    }

}
