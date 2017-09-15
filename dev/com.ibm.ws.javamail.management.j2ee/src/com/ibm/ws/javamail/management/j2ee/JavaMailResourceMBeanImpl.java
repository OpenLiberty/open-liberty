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
package com.ibm.ws.javamail.management.j2ee;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.ibm.websphere.management.j2ee.JavaMailResourceMBean;

/**
 * This MBean represents JavaMailResource and is registered on the MBeanServer by JavaMailResourceRegistrar
 */
public class JavaMailResourceMBeanImpl extends StandardMBean implements JavaMailResourceMBean {

    private final ObjectName objectName;

    public JavaMailResourceMBeanImpl(ObjectName objectName) {
        super(JavaMailResourceMBean.class, false);
        this.objectName = objectName;
    }

    /** {@inheritDoc} */
    @Override
    public String getobjectName() {
        return objectName == null ? "" : objectName.toString();
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

}
