/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Implementation of ConfigurationAdmin.
 * Provides APIs to managed Configurations such as providing APIs to create new
 * configuration
 * and get or list existing configurations.
 *
 */
class ConfigurationAdminImpl implements ConfigurationAdmin {
    private static final TraceComponent tc = Tr.register(ConfigurationAdminImpl.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);
    private final Bundle bundle;

    /** ConfigurationAdmin service factory */
    private final ConfigAdminServiceFactory caFactory;

    /**
     * Constructor.
     *
     * @param bndl
     * @param bc
     * @param ced
     * @param variableRegistry
     */
    public ConfigurationAdminImpl(ConfigAdminServiceFactory casf, Bundle bndl) {
        this.bundle = bndl;
        this.caFactory = casf;
    }

    /*
     * @see
     * org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.
     * lang.String)
     *
     * When a Configuration object is created by either getConfiguration or
     * createFactoryConfiguration, it becomes bound to the location of the calling
     * bundle. This location is obtained with the associated bundle's getLocation
     * method.
     */
    @Override
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid) throws IOException {
        ExtendedConfiguration config = caFactory.getConfigurationStore().createFactoryConfiguration(factoryPid, bundle.getLocation());
        return config;
    }

    /*
     * @see
     * org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.
     * lang.String, java.lang.String)
     *
     * In this call, create Configuration objects bound to the specified location,
     * instead of the location
     * of the calling bundle.
     *
     * @param factoryPid
     *
     * @param location
     */
    @Override
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) throws IOException {
        this.caFactory.checkConfigurationPermission();
        ExtendedConfiguration config = caFactory.getConfigurationStore().createFactoryConfiguration(factoryPid, location);
        return config;
    }

    /*
     * @see
     * org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String)
     *
     * Get an existing or new Configuration object from the persistent store.
     *
     * If the Configuration object for this PID does not exist, create a new
     * Configuration object for that PID, where properties are null.
     * Bind its location to the calling bundle's location.
     *
     * If the Configuration object for this PID does exist, and if the location of
     * the existing Configuration object is null,
     * set it to the calling bundle's location.
     *
     * SecurityException is thrown if the Configuration object is bound to a
     * location different from that of the calling bundle
     * and it does not have proper ConfigurationPermission.
     *
     * @param pid
     */
    @Override
    public ExtendedConfiguration getConfiguration(String pid) throws IOException {
        ExtendedConfigurationImpl config = caFactory.getConfigurationStore().getConfiguration(pid, bundle.getLocation());
        if (config.getBundleLocation(false) != null && !config.getBundleLocation(false).equals(bundle.getLocation()))
            this.caFactory.checkConfigurationPermission();

        if (config.isUnbound()) {
            Tr.warning(tc, "warning.binding.config", pid, bundle.getSymbolicName());
        }
        config.bind(bundle);
        return config;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String,
     * java.lang.String)
     *
     * If Configuration already exists(exists in table, or serialized), return the
     * existing configuration.
     * If existing configuration's location is null, set it with specified
     * location before returning it.
     *
     * If Configuration doesn't already exist, create a new Configuration objects
     * with null properties
     * and bound to the specified location including null location.
     *
     * SecurityException is thrown if caller doesn't have proper
     * ConfigurationPermission.
     *
     * @param pid
     *
     * @param location
     */
    @Override
    public ExtendedConfiguration getConfiguration(String pid, String location) throws IOException {
        this.caFactory.checkConfigurationPermission();
        return caFactory.getConfigurationStore().getConfiguration(pid, location);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.service.cm.ConfigurationAdmin#listConfigurations(java.lang.String)
     */
    @Override
    @FFDCIgnore(SecurityException.class)
    public ExtendedConfiguration[] listConfigurations(String filterString) throws InvalidSyntaxException {
        if (filterString == null)
            filterString = "(" + Constants.SERVICE_PID + "=*)"; //$NON-NLS-1$ //$NON-NLS-2$

        try {
            this.caFactory.checkConfigurationPermission();
        } catch (SecurityException e) {
            filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + bundle.getLocation() + ")" + filterString + ")";
        }
        return caFactory.getConfigurationStore().listConfigurations(FrameworkUtil.createFilter(filterString));
    }


	//
	//
    // R7 Upgrade
    //
    //

    public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {

		 // TODO: Maybe do list config, and create factory?  This new concept goes against our current
		 // IMPL..so need to think on it. Throwing IllegalStateException for now if called.

		throw new IllegalStateException("getFactoryConfiguration(String factoryPid, String name, String location) in ConfigurationAdminImpl.java has not been implemented.");

	}

	public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {

		//TODO: Similar comments as 3 parameter method.

		throw new IllegalStateException("getFactoryConfiguration(String factoryPid, String name) in ConfigurationAdminImpl.java has not been implemented.");
	}
}
