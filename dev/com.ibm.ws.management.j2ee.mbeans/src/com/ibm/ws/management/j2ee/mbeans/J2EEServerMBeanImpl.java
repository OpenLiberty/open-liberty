/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.management.j2ee.mbeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.J2EEServerMBean;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory.ModuleType;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory.ResourceType;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.management.j2ee.mbeans.internal.MBeanServerHelper;

/**
 * This MBean represents the J2EEServer and is registered on the MBeanServer by SMActivator
 */
public class J2EEServerMBeanImpl extends StandardMBean implements J2EEServerMBean {
    private static final String serverVendor = "IBM";
    private final String serverName;
    private final ObjectName objectName;

    public J2EEServerMBeanImpl(String serverName) {
        super(J2EEServerMBean.class, false);
        objectName = J2EEManagementObjectNameFactory.createJ2EEServerObjectName(serverName);
        this.serverName = serverName;
    }

    /** {@inheritDoc} */
    @Override
    public String getobjectName() {
        return objectName.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isstateManageable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isstatisticsProvider() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean iseventProvider() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getdeployedObjects() {
        ArrayList<String> deployedObjects = new ArrayList<String>();

        // Add all J2EEApplications MBeans
        deployedObjects.addAll(MBeanServerHelper.queryObjectName(
                        createPropertyPatternObjectName(J2EEManagementObjectNameFactory.TYPE_APPLICATION)));

        // Add all J2EEModules (including EJBModule, WebModule, AppClientModule and ResourceAdapterModule) MBeans
        List<ModuleType> allModuleTypes = Arrays.asList(J2EEManagementObjectNameFactory.ModuleType.values());
        for (ModuleType moduleType : allModuleTypes) {
            deployedObjects.addAll(MBeanServerHelper.queryObjectName(
                            createPropertyPatternObjectName(moduleType.name())));
        }

        return deployedObjects.toArray(new String[deployedObjects.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getresources() {
        ArrayList<String> resources = new ArrayList<String>();
        List<ResourceType> allResourceTypes = Arrays.asList(J2EEManagementObjectNameFactory.ResourceType.values());

        // Add all J2EEResources MBeans
        for (ResourceType resourceType : allResourceTypes) {
            resources.addAll(MBeanServerHelper.queryObjectName(
                            createPropertyPatternObjectName(resourceType.name())));
        }

        return resources.toArray(new String[resources.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getjavaVMs() {
        ArrayList<String> javaVMs = new ArrayList<String>();

        // There will be only one MBean registered
        javaVMs.addAll(MBeanServerHelper.queryObjectName(
                        createPropertyPatternObjectName(J2EEManagementObjectNameFactory.TYPE_JVM)));

        return javaVMs.toArray(new String[javaVMs.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public String getserverVendor() {
        return serverVendor;
    }

    /** {@inheritDoc} */
    @Override
    public String getserverVersion() {
        return getProductVersion();
    }

    private String getProductVersion() {
        Map<String, ProductInfo> productIdToVersionPropertiesMap = null;

        try {
            productIdToVersionPropertiesMap = ProductInfo.getAllProductInfo();

            for (ProductInfo versionProperties : productIdToVersionPropertiesMap.values()) {
                if (versionProperties.getReplacedBy() == null) {
                    return versionProperties.getVersion();
                }
            }
        } catch (ProductInfoParseException e) {
            // ignore exceptions - best effort to get a pretty string
        } catch (DuplicateProductInfoException e) {
            // ignore exceptions - best effort to get a pretty string
        } catch (ProductInfoReplaceException e) {
            // ignore exceptions - best effort to get a pretty string
        };

        return null;
    }

    private ObjectName createPropertyPatternObjectName(String j2eeType) {
        StringBuilder nameBuilder = new StringBuilder(J2EEManagementObjectNameFactory.DOMAIN_NAME).append(":");
        nameBuilder.append(J2EEManagementObjectNameFactory.KEY_TYPE).append("=").append(j2eeType).append(",");
        nameBuilder.append(J2EEManagementObjectNameFactory.TYPE_SERVER).append("=").append(serverName);
        nameBuilder.append(",*");

        ObjectName objectName = null;
        try {
            objectName = new ObjectName(nameBuilder.toString());
        } catch (MalformedObjectNameException e) {
            // ignore exceptions - This will never happen
        }

        return objectName;
    }
}
