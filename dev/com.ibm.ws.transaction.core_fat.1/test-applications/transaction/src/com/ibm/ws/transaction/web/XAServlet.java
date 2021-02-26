/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/XAServlet")
@Mode(TestMode.FULL)
public class XAServlet extends FATServlet {

    public void testSetTransactionTimeoutReturnsTrue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());

        tm.commit();
    }

    public void testSetTransactionTimeoutReturnsFalse(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setSetTransactionTimeoutAction(XAResourceImpl.RETURN_FALSE));

        tm.commit();
    }

    public void testSetTransactionTimeoutThrowsException(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setSetTransactionTimeoutAction(XAException.XAER_PROTO));
        tx.enlistResource(new XAResourceImpl().setSetTransactionTimeoutAction(XAException.XAER_PROTO));

        tm.commit();
    }

    @Test
    public void testXA001(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tm.commit();
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA002(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            // Should have rolledback
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA003(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA004(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    public void testXA005(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tx.setRollbackOnly();

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    public void testXA006(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tm.rollback();
    }

    @Test
    public void testXA007(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tx.setRollbackOnly();
        tm.rollback();
    }

    @Test
    public void testXA008(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        tm.commit();
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA009(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA010(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA011(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testXA012(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testXA013(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testXA014(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));

        tm.rollback();
    }

    private static final int HEURISTIC_RETRY_INTERVAL = 10;
    private static final int SUITABLE_DELAY = 5;

    public void testXA015(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        final int commitRepeatCount = 3;

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XAER_RMFAIL).setCommitRepeatCount(commitRepeatCount));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XAER_RMFAIL).setCommitRepeatCount(commitRepeatCount));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
            Thread.sleep(1000 * ((1 + commitRepeatCount) * HEURISTIC_RETRY_INTERVAL + SUITABLE_DELAY));
            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception();
            }
        }
    }
}