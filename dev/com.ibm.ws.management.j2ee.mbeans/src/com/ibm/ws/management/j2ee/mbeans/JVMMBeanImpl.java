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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.websphere.management.j2ee.JVMMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.management.j2ee.mbeans.internal.SMActivator;

/**
 * This MBean represents the JVM and is registered on the MBeanServer by SMActivator
 */
public class JVMMBeanImpl extends StandardMBean implements JVMMBean {

    private static final TraceComponent tc = Tr.register(JVMMBeanImpl.class, SMActivator.TRACE_GROUP_SM_MBEANS, SMActivator.TRACE_BUNDLE_SM_MBEANS);

    private final ObjectName objectName;
    private final String javaVersion, javaVendor, node;

    public JVMMBeanImpl(String serverName) {
        super(JVMMBean.class, false);
        objectName = J2EEManagementObjectNameFactory.createJVMObjectName(serverName);
        javaVersion = findJavaVersion();
        javaVendor = findJavaVendor();
        node = findNode();
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
    public String getjavaVersion() {
        return javaVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String getjavaVendor() {
        return javaVendor;
    }

    /** {@inheritDoc} */
    @Override
    public String getnode() {
        return node;
    }

    private static String getProperty(final String prop) {
        if (System.getSecurityManager() == null)
            return System.getProperty(prop);
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(prop);
                }
            });
    }

    private String findJavaVersion() {
        return getProperty("java.version");
    }

    private String findJavaVendor() {
        return getProperty("java.vendor");
    }

    private String findNode() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            @FFDCIgnore(UnknownHostException.class)
            public String run() {
                try {
                    return InetAddress.getLocalHost().getCanonicalHostName();
                } catch (UnknownHostException e) {
                    Tr.error(tc, "DETERMINE_HOST_ERROR", "localhost", e.getMessage());
                    return "localhost";
                }
            }
        });
    }

    @Override
    public String toString() {
        return "JVMMBeanImpl : objectName=" + objectName + " : javaVersion=" + javaVersion + " : javaVendor=" + javaVersion + " : node=" + node;
    }
}
