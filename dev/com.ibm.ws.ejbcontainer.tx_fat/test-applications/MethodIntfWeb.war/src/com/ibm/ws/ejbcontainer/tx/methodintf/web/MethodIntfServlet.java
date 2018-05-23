/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.tx.methodintf.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.ws.ejbcontainer.tx.methodintf.ejb.MethodIntfMDB;
import com.ibm.ws.ejbcontainer.tx.methodintf.ejb.MethodIntfSLSB;

import componenttest.app.FATServlet;

/**
 * This test ensures that the new method-intf element values specified in the
 * EJB 3.1 work properly.
 */
@SuppressWarnings("serial")
@WebServlet("/MethodIntfServlet")
public class MethodIntfServlet extends FATServlet {
    /**
     * This test verifies that method-intf = Timer applies to timeout callback
     * methods only.
     *
     * <p>An SLSB with { method-intf = Timer, method-name = *, trans-attribute =
     * NotSupported } is called via a business method that checks transaction
     * status. Then, a method is called to create a timer, wait for its
     * completion, and return the transaction status of the timeout callback
     * method.
     *
     * <p>The expected result is that the business method executes in a global
     * transaction, and the timeout callback method executes in a local
     * transaction.
     */
    @Test
    public void testTimer() throws Exception {
        MethodIntfSLSB bean = (MethodIntfSLSB) new InitialContext().lookup("java:app/MethodIntfEJB/MethodIntfSLSB");

        CountDownLatch cdLatch = bean.setup();

        assertTrue("timer never fired", cdLatch.await(50, TimeUnit.SECONDS));
        assertTrue("expected global transaction for business method", bean.isTransactionGlobal());
        assertFalse("expected local transaction for timeout method", bean.isTimeoutTransactionGlobal());
    }

    /**
     * This test verifies that method-intf = MessageEndpoint applies to message
     * listener interface methods only.
     *
     * <p>An MDB with { method-intf = MessageEndpoint, method-name = *,
     * trans-attribute = NotSupported } is sent a message, and then the test
     * waits for a latch. The endpoint method checks transaction status and
     * starts a timer. The timeout callback method checks transaction status
     * and notifies the latch.
     *
     * <p>The expected result is that the endpoint method executes in a local
     * transaction, and the timeout callback method executes in a global
     * transaction.
     */
    @Test
    public void testMessageEndpoint() throws Exception {
        MethodIntfMDB.setup();
        FATMDBHelper.putQueueMessage("message", "jms/MethodIntfMDBQCF", "jms/MethodIntfMDBQueue");
        MethodIntfMDB.svCountDownLatch.await(30, TimeUnit.SECONDS);

        assertFalse("expected local transaction for message", MethodIntfMDB.svMessageTransactionGlobal);
        assertTrue("expected global transaction for timeout", MethodIntfMDB.svTimeoutTransactionGlobal);
    }
}
