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
package org.apache.felix.scr.impl.helper;

import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.RegionConfigurationSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConfigAdminTracker
{
    public static final String CONFIGURATION_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";

    private final ServiceTracker<ConfigurationAdmin, RegionConfigurationSupport> configAdminTracker;

    public ConfigAdminTracker(final ComponentActivator componentActivator)
    {

        configAdminTracker = new ServiceTracker<>(
            componentActivator.getBundleContext(), CONFIGURATION_ADMIN,
            new ServiceTrackerCustomizer<ConfigurationAdmin, RegionConfigurationSupport>()
            {

                @Override
                public RegionConfigurationSupport addingService(ServiceReference<ConfigurationAdmin> reference)
                {
                    // let's do a quick check if the returned CA service is using the same
                    // CA API as is visible to this (SCR) bundle
                    boolean visible = false;
                    try
                    {
                        ConfigurationAdmin ca = componentActivator.getBundleContext().getService(reference);
                        if ( ca != null )
                        {
                            visible = true;
                            componentActivator.getBundleContext().ungetService(reference);
                        }
                    }
                    catch ( final Exception ex)
                    {
                            componentActivator.getLogger().log(Level.ERROR,
                                "Configuration admin API visible to bundle {0} is not the same as the Configuration Admin API visible to the SCR implementation.", ex, componentActivator.getBundleContext().getBundle());
                    }

                    if ( !visible )
                    {
                        return null;
                    }
                    return componentActivator.setRegionConfigurationSupport( reference );
                }

                @Override
                public void modifiedService(ServiceReference<ConfigurationAdmin> reference,
                    RegionConfigurationSupport service)
                {
                }

                @Override
                public void removedService(ServiceReference<ConfigurationAdmin> reference,
                    RegionConfigurationSupport rcs)
                {
                    componentActivator.unsetRegionConfigurationSupport( rcs );
                }
            } );

        configAdminTracker.open();
    }

    public void dispose()
    {
        configAdminTracker.close();
    }

}
