/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Set;
import java.util.concurrent.Future;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.Configuration.ConfigurationAttribute;
import org.osgi.framework.ServiceReference;

/**
 *
 */
public interface ExtendedConfiguration extends Configuration {

    public void lock();

    public void unlock();

    public void fireConfigurationDeleted(Collection<Future<?>> futureList);

    public void fireConfigurationUpdated(Collection<Future<?>> futureList);

    public void delete(boolean fireNotifications);

    public Object getProperty(String key);

    public Dictionary<String, Object> getReadOnlyProperties();

    public void updateCache(Dictionary<String, Object> properties, Set<ConfigID> references, Set<String> newUniques) throws IOException;

    public void updateProperties(Dictionary<String, Object> properties) throws IOException;

    public Set<ConfigID> getReferences();

    public void setInOverridesFile(boolean inOverridesFile);

    public boolean isInOverridesFile();

    public Set<String> getUniqueVariables();

    /**
     * Set the ConfigID that this configuration is registered under
     *
     * @param id
     */
    public void setFullId(ConfigID id);

    /**
     *
     * @return
     */
    public ConfigID getFullId();

    /**
     * Returns true if the configuration has been deleted
     *
     * @return true if the configuration has been deleted
     */
    public boolean isDeleted();


	//
    // R7 Upgrade
    //

    public Set<ConfigurationAttribute> getAttributes();

    public void addAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException;

	public void removeAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException;

	public boolean updateIfDifferent(java.util.Dictionary<java.lang.String,?> properties) throws java.io.IOException;

    public java.util.Dictionary<java.lang.String,java.lang.Object> getProcessedProperties(ServiceReference<?> reference);
}
