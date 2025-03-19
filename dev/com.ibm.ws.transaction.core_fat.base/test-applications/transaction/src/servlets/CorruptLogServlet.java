/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package servlets;

import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/CorruptLogServlet")
public class CorruptLogServlet extends FATServlet {

    @Resource
    private UserTransaction ut;

    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException" })
    public void testBasicCorruptLog() throws Exception {
        ut.begin();
        ut.commit();
    }

    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.InternalLogException", "java.lang.IllegalStateException" })
    public void test2PCDisabled() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        try {
            if (tx.enlistResource(new XAResourceImpl())) {
                fail("XA enlist succeeded unexpectedly");
            }

            fail("XA enlist did not throw IllegalStateException");
        } catch (IllegalStateException e) {
            // This is what we expect
        }
    }
}
