/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.serverxml;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.common.InternalConfigSource;

/**
 * A ConfigSource which returns values from appProperties elements in the server.xml file e.g.
 *
 * <application location="serverXMLApp.war">
 * <appProperties>
 * <property name="serverXMLKey1" value="serverXMLValue1"/>
 * <property name="serverXMLKey1" value="serverXMLValue1a"/>
 * <property name="serverXMLKey2" value="serverXMLValue2"/>
 * <property name="serverXMLKey3" value="serverXMLValue3"/>
 * <property name="serverXMLKey1" value="serverXMLValue1b"/>
 * <property name="serverXMLKey4" value="serverXMLValue4"/>
 * </appProperties>
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
public class AppPropertyConfigSource extends InternalConfigSource implements ConfigSource { //extends InternalConfigSource implements DynamicConfigSource {

    private static final TraceComponent tc = Tr.register(AppPropertyConfigSource.class);
    private BundleContext bundleContext;
    private String applicationName;
    private final ConfigAction configAction;
    private ConfigurationAdmin configurationAdmin;
    private String applicationPID;

    public AppPropertyConfigSource() {
        super(Config13Constants.APP_PROPERTY_ORDINAL, Tr.formatMessage(tc, "server.xml.appproperties.config.source"));
        this.configAction = new ConfigAction();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = new HashMap<String, String>();

        SortedSet<Configuration> osgiConfigs = null;

        if (System.getSecurityManager() == null) {
            osgiConfigs = getOSGiConfigurations();
        } else {
            osgiConfigs = AccessController.doPrivileged(this.configAction);
        }

        if (osgiConfigs != null) {//osgiConfigs could be null if not inside an OSGi framework, e.g. unit test
            for (Configuration osgiConfig : osgiConfigs) {
                // Locate name/value pairs in the config objects and place them in the map.
                Dictionary<String, Object> dict = osgiConfig.getProperties();
                Enumeration<String> keys = dict.keys();

                Object myKey = null;
                Object myValue = null;
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();

                    if (key.equals("name"))
                        myKey = dict.get(key);
                    if (key.equals("value"))
                        myValue = dict.get(key);
                    if (myKey != null && myValue != null) {
                        props.put(myKey.toString(), myValue.toString());
                    }
                }
            }
        }

        return props;
    }

    private BundleContext getBundleContext() {
        if (this.bundleContext == null) {
            this.bundleContext = OSGiConfigUtils.getBundleContext(getClass());
        }
        return this.bundleContext;
    }

    @FFDCIgnore(InvalidFrameworkStateException.class)
    protected ConfigurationAdmin getConfigurationAdmin() {
        if (this.configurationAdmin == null) {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext != null) {
                try {
                    this.configurationAdmin = OSGiConfigUtils.getConfigurationAdmin(bundleContext);
                } catch (InvalidFrameworkStateException e) {
                    //OSGi framework is shutting down, ignore and return null;
                }
            }
        }
        return this.configurationAdmin;
    }

    private String getApplicationName() {
        if (this.applicationName == null) {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext != null) { //bundleContext could be null if not inside an OSGi framework, e.g. unit test
                this.applicationName = OSGiConfigUtils.getApplicationName(bundleContext);
            }
        }
        return this.applicationName;
    }

    private String getApplicationPID() {
        if (this.applicationPID == null) {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext != null) { //bundleContext could be null if not inside an OSGi framework, e.g. unit test
                this.applicationPID = OSGiConfigUtils.getApplicationPID(bundleContext, applicationName);
            }
        }
        return this.applicationPID;
    }

    private class ConfigAction implements PrivilegedAction<SortedSet<Configuration>> {

        /** {@inheritDoc} */
        @Override
        public SortedSet<Configuration> run() {
            return getOSGiConfigurations();
        }

    }

    private SortedSet<Configuration> getOSGiConfigurations() {
        SortedSet<Configuration> osgiConfigs = null;
        BundleContext bundleContext = getBundleContext();
        if (bundleContext != null) { //bundleContext could be null if not inside an OSGi framework, e.g. unit test

            ConfigurationAdmin configurationAdmin = getConfigurationAdmin();
            if (configurationAdmin != null) {

                String applicationName = getApplicationName();

                //if the Config is being obtained outside the context of an application then the applicationName may be null
                //in which case there are no applicable application configuration elements to return
                if (applicationName != null) {
                    String applicationPID = getApplicationPID();
                    if (applicationPID != null) {
                        osgiConfigs = OSGiConfigUtils.getConfigurations(configurationAdmin, applicationPID);
                    }
                }
            }
        }
        return osgiConfigs;
    }

}
