/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

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
public class AppPropertyConfigSource extends InternalConfigSource {

    private static final TraceComponent tc = Tr.register(AppPropertyConfigSource.class);

    private final PrivilegedAction<String> getApplicationPidAction = new GetApplicationPidAction();
    private final String name;
    
    private BundleContext bundleContext;
    private String applicationName;
    private String applicationPID;

    @Trivial
    public AppPropertyConfigSource() {
        name = Tr.formatMessage(tc, "server.xml.appproperties.config.source");
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ServerXMLConstants.APP_PROPERTY_ORDINAL;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String key) {
        return getProperties().get(key);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {

        String appPid;
        if (System.getSecurityManager() == null) {
            appPid = getApplicationPID();
        } else {
            appPid = AccessController.doPrivileged(getApplicationPidAction);
        }

        if (appPid != null) {
            return AppPropertiesTrackingComponent.getAppProperties(appPid);
        } else {
            return Collections.emptyMap();
        }
    }

    private BundleContext getBundleContext() {
        if (this.bundleContext == null) {
            this.bundleContext = OSGiConfigUtils.getBundleContext(getClass());
        }
        return this.bundleContext;
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
                this.applicationPID = OSGiConfigUtils.getApplicationPID(bundleContext, getApplicationName());
            }
        }
        return this.applicationPID;
    }

    private class GetApplicationPidAction implements PrivilegedAction<String> {
        @Override
        public String run() {
            return getApplicationPID();
        }
    }

}
