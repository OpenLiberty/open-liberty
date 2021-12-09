/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.OnePhaseXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ForcePrepareServlet")
public class ForcePrepareServlet extends FATServlet {

    private final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

    Status status;

    @Resource
    TransactionSynchronizationRegistry tsr;

    @Test
    public void control1(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void afterCompletion(int status) {
                assertEquals(status, Status.STATUS_COMMITTED);
            }

            @Override
            public void beforeCompletion() {
            }
        });

        final Transaction tx = tm.getTransaction();

        XAResourceImpl resource = new OnePhaseXAResourceImpl();

        tx.enlistResource(resource);

        try {
            tm.commit();
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        assertTrue("One phase commit did not occur on one phase resource", resource.inState(XAResourceImpl.COMMITTED_ONE_PHASE));
    }

    @Test
    public void control2(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void afterCompletion(int status) {
                assertEquals(status, Status.STATUS_COMMITTED);
            }

            @Override
            public void beforeCompletion() {
            }
        });

        final Transaction tx = tm.getTransaction();

        XAResourceImpl resource = new OnePhaseXAResourceImpl();
        XAResourceImpl resource2 = new XAResourceImpl();

        tx.enlistResource(resource);
        tx.enlistResource(resource2);

        try {
            tm.commit();
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        assertTrue("One phase commit did not occur on one phase resource", resource.inState(XAResourceImpl.COMMITTED_ONE_PHASE));
    }

    @Test
    public void testForcePrepareOneResource(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void afterCompletion(int status) {
                assertEquals(status, Status.STATUS_COMMITTED);
            }

            @Override
            public void beforeCompletion() {
            }
        });

        final Transaction tx = tm.getTransaction();

        XAResourceImpl resource = new XAResourceImpl();

        tx.enlistResource(resource);

        try {
            tm.commit();
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        assertFalse("Two phase resource was not prepared", resource.inState(XAResourceImpl.COMMITTED_ONE_PHASE));
    }

    @Test
    @ExpectedFFDC({ "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testForcePrepareRollback(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void afterCompletion(int status) {
                assertEquals(status, Status.STATUS_ROLLEDBACK);
            }

            @Override
            public void beforeCompletion() {
            }
        });

        final Transaction tx = tm.getTransaction();

        XAResourceImpl resource = new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK);

        tx.enlistResource(resource);

        try {
            tm.commit();
        } catch (RollbackException e) {
            // expected
            e.printStackTrace();
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        assertFalse("Two phase resource was not prepared", resource.inState(XAResourceImpl.ROLLEDBACK));
    }

    @Test
    public void testForcePrepareOptimized(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void afterCompletion(int status) {
                assertEquals(status, Status.STATUS_COMMITTED);
            }

            @Override
            public void beforeCompletion() {
            }
        });

        final Transaction tx = tm.getTransaction();

        XAResourceImpl resource1 = new XAResourceImpl();
        XAResourceImpl resource2 = new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY);

        tx.enlistResource(resource1);
        tx.enlistResource(resource2);

        try {
            tm.commit();
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        assertFalse("Two phase resource was not prepared", resource1.inState(XAResourceImpl.COMMITTED_ONE_PHASE));
    }
}