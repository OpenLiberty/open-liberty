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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ModuleProperties {
    private Map<Class, Properties> authMechMap;
    private Map<Class, Properties> identityStoreMap;

    public ModuleProperties(Map<Class, Properties> authMechMap, Map<Class, Properties> identityStoreMap) {
        this.authMechMap = authMechMap;
        this.identityStoreMap = identityStoreMap;
    }

    public ModuleProperties() {
        authMechMap = new HashMap<Class, Properties>();
        identityStoreMap = new HashMap<Class, Properties>();
    }

    public Map<Class, Properties> getAuthMechMap() {
        return authMechMap;
    }
    public Properties getFromAuthMechMap(Class className) {
        return authMechMap.get(className);
    }

    public Properties putToAuthMechMap(Class className, Properties props) {
        return authMechMap.put(className, props);
    }

    public Map<Class, Properties> getIdentityStoreMap() {
        return identityStoreMap;
    }

    public Properties getFormIdentityStoreMap(Class className) {
        return identityStoreMap.get(className);
    }

    public Properties putToIdentityStoreMap(Class className, Properties props) {
        return identityStoreMap.put(className, props);
    }

}
