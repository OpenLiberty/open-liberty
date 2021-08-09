/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This class defines the interface for passing configuration data
 * containing a set of role mappings
 * 
 * @see AuthorizationService
 * @see AuthorizationTableService
 * @see RoleSet
 */
public interface AuthorizationTableConfigService {

    /**
     * Set the role mappings from the config
     * 
     * @param configurationAdmin
     * 
     * @param properties the properties from the pid class activate/modified methods
     */
    void setConfiguration(String[] roleNames, ConfigurationAdmin configurationAdmin, Map<String, Object> properties);
}
