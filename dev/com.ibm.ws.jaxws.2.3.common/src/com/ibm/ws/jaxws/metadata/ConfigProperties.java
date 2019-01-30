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
package com.ibm.ws.jaxws.metadata;

import java.util.Hashtable;
import java.util.Map;

/**
 * Using the class to present the properties of the configuration
 */
public class ConfigProperties {
    /*
     * The factory pid of the configuration
     */
    private final String factoryPid;

    private final Hashtable<String, String> properties = new Hashtable<String, String>();

    public ConfigProperties(String factoryPid, Map<String, String> properties) {
        this.factoryPid = factoryPid;
        this.properties.putAll(properties);
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public Hashtable<String, String> getProperties() {
        return new Hashtable<String, String>(properties);
    }

    public void putProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    public void putProperty(String key, String value) {
        this.properties.put(key, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((factoryPid == null) ? 0 : factoryPid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigProperties other = (ConfigProperties) obj;
        if (factoryPid == null) {
            if (other.factoryPid != null)
                return false;
        } else if (!factoryPid.equals(other.factoryPid))
            return false;
        return true;
    }

}
