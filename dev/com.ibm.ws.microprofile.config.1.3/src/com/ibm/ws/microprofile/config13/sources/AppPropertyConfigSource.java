/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.sources.DynamicConfigSource;
import com.ibm.ws.microprofile.config.sources.InternalConfigSource;
import com.ibm.ws.microprofile.config13.interfaces.Config13Constants;

/**
 * A ConfigSource which returns values from appProperties elements in the server.xml file
 */
public class AppPropertyConfigSource extends InternalConfigSource implements DynamicConfigSource {

    private static final TraceComponent tc = Tr.register(AppPropertyConfigSource.class);

    private Configuration osgiConfig;

    public AppPropertyConfigSource() {
        super(Config13Constants.APP_PROPERTY_ORDINAL, Tr.formatMessage(tc, "server.xml.appproperties.config.source"));

    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = new HashMap<String, String>();

        Configuration osgiConfig = getOSGiConfiguration();
        if (osgiConfig != null) {
            Dictionary<String, Object> dict = osgiConfig.getProperties();

            Enumeration<String> keys = dict.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (!OSGiConfigUtils.isSystemKey(key)) {
                    Object value = dict.get(key);
                    props.put(key, value.toString());
                }
            }
        }
        return props;
    }

    private Configuration getOSGiConfiguration() {
        if (this.osgiConfig == null) {
            PrivilegedAction<Configuration> configAction = () -> {
                BundleContext bundleContext = OSGiConfigUtils.getBundleContext(getClass());
                String applicationName = OSGiConfigUtils.getApplicationName(bundleContext);
                Configuration osgiConfig = OSGiConfigUtils.getConfiguration(bundleContext, applicationName);

                return osgiConfig;
            };

            this.osgiConfig = AccessController.doPrivileged(configAction);

        }
        return this.osgiConfig;
    }
}
