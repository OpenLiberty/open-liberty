/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.tx;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionManager;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/RemoteTxTestServlet")
@SuppressWarnings("serial")
public class RemoteTxTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(RemoteTxTestServlet.class.getName());

    @EJB(beanName = "BusinessTxRemoteStatelessBean")
    private BusinessTxRemote businessRemoteStateless;

    @Test
    public void testBusinessRemoteTranRequiredNoContext() throws Exception {
        businessRemoteStateless.testTransactionRequired();
    }

    // Note: After the implementation of 173156 - Co-located server distributed transaction support - the
    // behaviour of this test has changed. A transaction will be "imported" by the remote EJB.
    @Test
    public void testBusinessRemoteTranRequiredWithContext() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        businessRemoteStateless.testTransactionRequired();
        tm.commit();
    }

    @Test
    public void testBusinessRemoteTranSupportsNoContext() throws Exception {
        businessRemoteStateless.testTransactionSupports();
    }

    // Note: After the implementation of 173156 - Co-located server distributed transaction support - the
    // behaviour of this test has changed. A transaction will be "imported" by the remote EJB.
    @Test
    public void testBusinessRemoteTranSupportsWithContext() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        businessRemoteStateless.testTransactionSupports();
        tm.commit();
    }

    // Note - we could distinguish this case from the case where we provide a transaction context by parsing the exception message.
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException", })
    public void testBusinessRemoteTranMandatoryNoContext() throws Exception {
        try {
            businessRemoteStateless.testTransactionMandatory();
            throw new IllegalStateException("expected EJBTransactionRequiredException");
        } catch (javax.ejb.EJBTransactionRequiredException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    // Note: After the implementation of 173156 - Co-located server distributed transaction support - the
    // behaviour of this test has changed. A transaction will be "imported" by the remote EJB.
    @Test
    public void testBusinessRemoteTranMandatoryWithContext() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        businessRemoteStateless.testTransactionMandatory();
        tm.commit();
    }

    @Test
    public void testBusinessRemoteTranNotSupportedNoContext() throws Exception {
        businessRemoteStateless.testTransactionNotSupported();
    }

    @Test
    public void testBusinessRemoteTranNotSupportedWithContext() throws Exception {

        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        businessRemoteStateless.testTransactionNotSupported();
        tm.commit();
    }
}
