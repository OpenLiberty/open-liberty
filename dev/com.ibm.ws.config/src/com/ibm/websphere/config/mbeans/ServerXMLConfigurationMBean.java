/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.config.mbeans;

import java.util.Collection;

/**
 * The ServerXMLConfigurationMBean provides an interface for retrieving the file paths
 * of all of the server configuration files known to the server.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * 
 * @ibm-api
 */
public interface ServerXMLConfigurationMBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    public static final String OBJECT_NAME = "WebSphere:name=com.ibm.websphere.config.mbeans.ServerXMLConfigurationMBean";

    /**
     * Fetches and returns a collection containing the file paths of all the server
     * configuration files known to the server.
     * 
     * @return an unordered collection of server configuration file paths
     */
    public Collection<String> fetchConfigurationFilePaths();

}
