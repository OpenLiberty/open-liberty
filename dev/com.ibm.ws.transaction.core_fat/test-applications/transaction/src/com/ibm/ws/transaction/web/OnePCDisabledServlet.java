/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/OnePCDisabledServlet")
public class OnePCDisabledServlet extends FATServlet {

    @Test
    public void test1PCOptimizationDisabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();
        final Transaction tx = tm.getTransaction();

        XAResourceImpl readOnlyVoter = new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY);
        XAResourceImpl okVoter = new XAResourceImpl();

        tx.enlistResource(okVoter);
        tx.enlistResource(readOnlyVoter);

        tm.commit();

        if (okVoter.inState(XAResourceImpl.COMMITTED_ONE_PHASE)) {
            throw new Exception("One Phase Optimization occurred when it is supposed to be disabled");
        }
    }
}