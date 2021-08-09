/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.fat.remoteTester;

import java.io.Serializable;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@Component(immediate = true, service = XAResourceFactory.class, property = { "testfilter=wsat" })
public class RemoteTestService1 implements XAResourceFactory {

    protected static final TraceComponent tc = Tr.register(RemoteTestService1.class);

    @Reference
    protected void setRemoteTransactionController(RemoteTransactionController rtc) {
        System.out.println("service: setRemoteTransactionController: " + rtc);
        RemoteTestServlet.setRemoteTransactionController(rtc);
    }

    @Reference
    protected void setTransactionManager(TransactionManager tm) {
        System.out.println("service: setTransactionManager: " + tm);
        RemoteTestServlet.setTransactionManager(tm);
    }

    private final AtomicServiceReference<RemoteTransactionController> remoteTransactionControllerRef = new AtomicServiceReference<RemoteTransactionController>("RTC");

    @Activate
    protected void activate(final ComponentContext context) throws InvalidSyntaxException {
        System.out.println("service: Activating RemoteTransactionController");
        remoteTransactionControllerRef.activate(context);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#getXAResource(java.io.Serializable)
     */
    @Override
    public XAResource getXAResource(Serializable xaResInfo) throws XAResourceNotAvailableException {

        final XAResourceImpl res = (XAResourceImpl) XAResourceFactoryImpl.instance().getXAResource(xaResInfo);

        return res;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#destroyXAResource(javax.transaction.xa.XAResource)
     */
    @Override
    public void destroyXAResource(XAResource xaRes) throws DestroyXAResourceException {
        // TODO Auto-generated method stub

    }
}