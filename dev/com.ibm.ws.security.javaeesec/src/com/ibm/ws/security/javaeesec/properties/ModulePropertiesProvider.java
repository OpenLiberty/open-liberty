/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.properties;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public interface ModulePropertiesProvider {
    /**
     * returns the ModuleProperties which is associated with the current ModuleMetaData.
     */
    public ModuleProperties getModuleProperties();
    
    /**
     * returns the list of AuthenticationMechanisms which is associated with the current ModuleMetaData.
     */
    public List<Class> getAuthMechClassList();

    /**
     * returns the Properties of AuthenticationMechanisms.
     */
    public Properties getAuthMechProperties(Class authMech);
}
