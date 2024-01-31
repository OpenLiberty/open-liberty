/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

//@formatter:off
class ConfigurationAdminImpl implements ConfigurationAdmin {

    public ConfigurationAdminImpl(ConfigAdminServiceFactory caFactory, Bundle bundle) {
        this.bundle = bundle;
        this.caFactory = caFactory;
    }

    private final Bundle bundle;

    private final ConfigAdminServiceFactory caFactory;

    @Trivial
    private void checkPermission() {
        caFactory.checkConfigurationPermission();
    }

    /*
     * Create and bind a configuration.
     *
     * Generate a factory specific ID for the new configuration.  Store the new
     * configuration in the configuration store.
     *
     * @param factoryPid The PID of the factory used to create the configuration.
     * @return The new configuration.
     */
    @Override
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid) throws IOException {
        // TODO: Check permissions?
        return caFactory.getConfigurationStore().createFactoryConfiguration(factoryPid, bundle);
    }

    /*
     * Create and bind a configuration.
     *
     * Generate a factory specific ID for the new configuration.  Store the new
     * configuration in the configuration store.
     *
     * Throw an exception if sufficient permissions are not available.
     *
     * @param factoryPid The PID of the factory used to create the configuration.
     * @param location The location of the configuration.  Usually a bundle location.
     *
     * @return The new configuration.
     */
    @Override
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) throws IOException {
        checkPermission();
        return caFactory.getConfigurationStore().createFactoryConfiguration(factoryPid, location);
    }

    /*
     * Get or create a configuration.
     *
     * If a location is set which is different than set bundle location,
     * check permissions.
     *
     * If unbound, issue a warning and bind the configuration to the
     * admin's bundle.
     *
     * @param pid  The PID of the configuration which is to retrieved.
     */
    @Override
    public ExtendedConfiguration getConfiguration(String pid) throws IOException {
        ExtendedConfigurationImpl config = caFactory.getConfiguration(pid, bundle);
        config.checkBinding(bundle);
        return config;
    }

    /*
     * See: {@link ConfigurationAdmin#getConfiguration(String, String)}.
     *
     * If Configuration already exists(exists in table, or serialized), return the
     * existing configuration.
     *
     * If existing configuration's location is null, set it with specified
     * location before returning it.
     *
     * If Configuration doesn't already exist, create a new Configuration objects
     * with null properties and bound to the specified location including null location.
     *
     * SecurityException is thrown if caller doesn't have proper ConfigurationPermission.
     */
    @Override
    public ExtendedConfiguration getConfiguration(String pid, String location) throws IOException {
        checkPermission();

        return caFactory.getConfigurationStore().getConfiguration(pid, location);
    }

    @Override
    @FFDCIgnore(SecurityException.class)
    public ExtendedConfiguration[] listConfigurations(String filterString) throws InvalidSyntaxException {
        if ( filterString == null ) {
            filterString = "(" + Constants.SERVICE_PID + "=*)";
        }

        try {
            checkPermission();
        } catch ( SecurityException e ) {
            filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + bundle.getLocation() + ")" + filterString + ")";
        }

        Filter filter = FrameworkUtil.createFilter(filterString);

        return caFactory.getConfigurationStore().listConfigurations(filter);
    }

    // TODO: Maybe do list config, and create factory?  This new concept goes against our current
    // IMPL..so need to think on it.
    @Override
    public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {
        throw new UnsupportedOperationException();
    }

    //TODO: Similar comments as 3 parameter method.
    @Override
    public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {
        throw new UnsupportedOperationException();
    }
}
//@formatter:on