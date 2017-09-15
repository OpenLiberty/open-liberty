/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tx.admin.internal;

import com.ibm.websphere.management.j2ee.JTAResourceMBean;
import com.ibm.ws.tx.admin.TransactionServiceMBean;

public class TransactionServiceMBeanImpl implements TransactionServiceMBean, JTAResourceMBean {

    private final String objectName;

    TransactionServiceMBeanImpl(String objectName) {
        this.objectName = objectName;
    }

    /** {@inheritDoc} */
    @Override
    public String getobjectName() {
        return objectName;
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