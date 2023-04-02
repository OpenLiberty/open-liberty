/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

package com.ibm.websphere.simplicity.configuration;

import java.io.File;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link ConfigurationProvider} for a properties configuration file. This provider uses the
 * {@link FormattedProperties} Object to manage and write properties.
 */
public class PropertiesConfigurationProvider extends ConfigurationProvider {
    
    FormattedProperties props;

    /**
     * Constructor
     * 
     * @param configFile The properties configuration file to load
     * @throws Exception
     */
	public PropertiesConfigurationProvider(File configFile) throws Exception {
		super(configFile);
        props = new FormattedProperties();
        props.load(getInputStream());
	}
	
	@Override
	public boolean hasProperty(String property) {
        return props.containsKey(property);
	}

	@Override
	public String getProperty(String property) {
		return props.getProperty(property);
	}

	@Override
	public Properties getProperties(String key) {
		Properties p = new Properties();
		for (Map.Entry<Object, Object> e : props.entrySet()) {
			String ekey = (String)e.getKey();
			if (ekey.startsWith(key)) {
				p.put(e.getKey(), e.getValue());
			}
		}
		return p;
	}

	@Override
	public void setProperty(String property, String value) {
		props.setProperty(property, value);	
	}

    @Override
    public Enumeration<?> getPropertyNames() {
        return props.propertyNames();
    }

    @Override
    public void writeProperties() throws Exception {
        OutputStream os = getOutputStream(false);
        props.store(os, null);
        os.close();
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        for(String prop : properties.keySet()) {
            props.setProperty(prop, properties.get(prop));
        }
    }

    @Override
    public void removeProperty(String property) {
        props.remove(property);
    }

    @Override
    public void reloadProperties() throws Exception {
        props.clear();
        props.load(getInputStream());
    }

}
