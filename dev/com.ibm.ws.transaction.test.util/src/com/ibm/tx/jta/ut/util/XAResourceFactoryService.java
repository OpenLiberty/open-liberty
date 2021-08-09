/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.io.Serializable;

import javax.transaction.xa.XAResource;

import org.osgi.service.component.annotations.Component;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;

/**
 *
 */
// This property is used in the filter to look up an XAResourceFactory instance.
// In registerResourceInfo the equivalent filter for this service is "(testfilter=jon)". See web.RecoveryServlet
// The code to look this up is in XARecoveryDataHelper.lookupXAResourcefactory()
@Component(service = XAResourceFactory.class, property = { "testfilter=jon" })
public class XAResourceFactoryService implements XAResourceFactory {

    /** {@inheritDoc} */
    @Override
    public void destroyXAResource(XAResource arg0) throws DestroyXAResourceException {
        XAResourceFactoryImpl.instance().destroyXAResource(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public XAResource getXAResource(Serializable arg0) throws XAResourceNotAvailableException {
        return XAResourceFactoryImpl.instance().getXAResource(arg0);
    }
}