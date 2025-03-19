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

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.OnePhaseXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BadLogServlet")
public class BadLogServlet extends FATServlet {

    @Test
    public void testBasicBadLog() throws Exception {
        final Object ut = new InitialContext().lookup("java:comp/UserTransaction");

        if (ut instanceof UserTransaction) {
            ((UserTransaction) ut).begin();
            ((UserTransaction) ut).commit();
        } else {
            if (ut == null) {
                throw new Exception("UserTransaction instance was null");
            } else {
                throw new Exception("UserTransaction lookup did not work: " + ut.getClass().getCanonicalName());
            }
        }
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void test2PCDisabled() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();

        final Transaction tx = tm.getTransaction();

        try {
            tx.enlistResource(new XAResourceImpl());
            fail("XA enlist succeeded unexpectedly");
        } catch (IllegalStateException e) {
        }

        tm.rollback();
    }

    @Test
    public void test1PCEnabled() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new OnePhaseXAResourceImpl());

        tm.commit();
    }
}

