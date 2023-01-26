/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.longtran.web;

import java.io.Serializable;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;
import javax.enterprise.concurrent.ManagedExecutorService;

//import javax.enterprise.concurrent.ManagedExecutorService;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/LongtranServlet")
public class LongtranServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

    /**
     * Invoke a single transaction
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public void normalTran(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            // Drive commit
            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Resource
    ManagedExecutorService managedExecutorService;

    /**
     * Begin and stall a transaction in a spawned thread.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public void longRunningTran(HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        // submit transactional work to the Executor
        managedExecutorService.submit(new LongRunningWork());
    }

    public class LongRunningWork implements Runnable {

        @Override
        public void run() {
            final ExtendedTransactionManager tm = TransactionManagerFactory
                            .getTransactionManager();

            try {
                tm.begin();

                // Now pause for 15 seconds, meantime the server will be shutdown
                Thread.sleep(15 * 1000);
                // Drive commit
                tm.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
