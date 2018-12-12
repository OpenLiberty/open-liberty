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

import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.ws.ejbcontainer.tx.methodintf.ejb.MethodIntfMDB;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * This test ensures that the new method-intf element values specified in the
 * EJB 3.1 work properly.
 */
@SuppressWarnings("serial")
@WebServlet("/SerialFilterEnabledServlet")
public class SerialFilterEnabledServlet extends FATServlet {
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
    @ExpectedFFDC("java.io.InvalidClassException")
    public void testMessageEndpoint() throws Exception {
        try {
            MethodIntfMDB.setup();
            FATMDBHelper.putQueueMessage("message", "jms/MethodIntfMDBQCF", "jms/MethodIntfMDBQueue");
            MethodIntfMDB.svCountDownLatch.await(30, TimeUnit.SECONDS);
        } catch (java.io.InvalidClassException e) {
            System.out.println("This exception is expected if the serialFilter is enforced");
        }

    }
}