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

import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.ibm.websphere.management.j2ee.J2EEDomainMBean;
import com.ibm.websphere.management.j2ee.J2EEManagementObjectNameFactory;
import com.ibm.ws.management.j2ee.mbeans.internal.MBeanServerHelper;

/**
 * This MBean represents the J2EEDomain and is registered on the MBeanServer by SMActivator
 */
public class J2EEDomainMBeanImpl extends StandardMBean implements J2EEDomainMBean {
    private final ObjectName objectName;

    public J2EEDomainMBeanImpl() {
        super(J2EEDomainMBean.class, false);
        objectName = J2EEManagementObjectNameFactory.createJ2EEDomainObjectName();
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
    public String[] getservers() {
        List<String> servers = MBeanServerHelper.queryObjectName(
                        J2EEManagementObjectNameFactory.createJ2EEServerObjectName("*"));

        return servers.toArray(new String[servers.size()]);
    }

    @Override
    public String toString() {
        return "J2EEDomainMBeanImpl : objectName=" + objectName + " : servers=" + Arrays.toString(getservers());
    }
}