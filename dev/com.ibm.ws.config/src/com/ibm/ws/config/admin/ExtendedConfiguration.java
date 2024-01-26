/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

/**
 * Liberty extension to the core OSGi service configuration type.
 */
//@formatter:off
public interface ExtendedConfiguration extends Configuration {
    void lock();
    void unlock();

    void setFullId(ConfigID id);
    ConfigID getFullId();

    void setInOverridesFile(boolean inOverridesFile);
    boolean isInOverridesFile();

    boolean isDeleted();
    void delete(boolean fireNotifications);
    void fireConfigurationDeleted(Collection<Future<?>> futures);

    void updateCache(Dictionary<String, Object> properties, Set<ConfigID> references, Set<String> newUniques) throws IOException;
    void updateProperties(Dictionary<String, Object> properties) throws IOException;
    void fireConfigurationUpdated(Collection<Future<?>> futures);

    Object getProperty(String key);
    Dictionary<String, Object> getReadOnlyProperties();

    Set<ConfigID> getReferences();
    Set<String> getUniqueVariables();
}
//@formatter:on
