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
import java.util.SortedSet;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.sources.DynamicConfigSource;
import com.ibm.ws.microprofile.config.sources.InternalConfigSource;
import com.ibm.ws.microprofile.config13.interfaces.Config13Constants;

/**
 * A ConfigSource which returns values from appProperties elements in the server.xml file e.g.
 *
 * <application location="serverXMLApp.war">
 * <appProperties serverXMLKey1="serverXMLValue1"/>
 * <appProperties serverXMLKey1="serverXMLValue1a" serverXMLKey2="serverXMLValue2" serverXMLKey3="serverXMLValue3"/>
 * <appProperties serverXMLKey1="serverXMLValue1b" serverXMLKey4="serverXMLValue4"/>
 * </application>
 *
 * Result should be
 *
 * serverXMLKey1="serverXMLValue1b"
 * serverXMLKey2="serverXMLValue2"
 * serverXMLKey3="serverXMLValue3"
 * serverXMLKey4="serverXMLValue4"
 *
 * Note that serverXMLKey1 was listed three times and the last entry "won"
 *
 */
public class AppPropertyConfigSource extends InternalConfigSource implements DynamicConfigSource {

    private static final TraceComponent tc = Tr.register(AppPropertyConfigSource.class);
    private BundleContext bundleContext;
    private String applicationName;

    public AppPropertyConfigSource() {
        super(Config13Constants.APP_PROPERTY_ORDINAL, Tr.formatMessage(tc, "server.xml.appproperties.config.source"));
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = new HashMap<String, String>();

        SortedSet<Configuration> osgiConfigs = getOSGiConfigurations();
        for (Configuration osgiConfig : osgiConfigs) {

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

    private SortedSet<Configuration> getOSGiConfigurations() {
        PrivilegedAction<SortedSet<Configuration>> configAction = () -> {
            if (bundleContext == null) {
                bundleContext = OSGiConfigUtils.getBundleContext(getClass());
            }
            if (applicationName == null) {
                applicationName = OSGiConfigUtils.getApplicationName(bundleContext);
            }
            SortedSet<Configuration> osgiConfigs = OSGiConfigUtils.getConfigurations(bundleContext, applicationName);

            return osgiConfigs;
        };

        SortedSet<Configuration> osgiConfigs = AccessController.doPrivileged(configAction);

        return osgiConfigs;
    }
}
