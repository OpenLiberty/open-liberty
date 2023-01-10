/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ddTimeout.web;

import static org.junit.Assert.fail;

import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ddTimeoutTestServlet")
public class DDTimeoutTestServlet extends FATServlet {

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.transaction.TransactionRolledbackException" })
    public void testDDTimeout() throws NamingException, InterruptedException {

        final TestEJB t = (TestEJB) new InitialContext().lookup("java:module/TestEJB");

        try {
            t.method();
            fail("Transaction did not timeout!");
        } catch (EJBTransactionRolledbackException e) {
            if (e.getMessage().indexOf("Transaction is ended due to timeout") < 0) {
                System.out.println("Transaction rollback was not caused by a timeout");
                throw e;
            }
        }
    }
}