/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.RollbackException;

import org.junit.Test;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.AbortableXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TimeoutServlet")
public class TimeoutServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

    @Test
    @AllowedFFDC(value = { "javax.transaction.RollbackException" })
    public void testTransactionTimeoutWithNonAbortableResource(HttpServletRequest request,
                                                               HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);
        Date date = null;
        long startTime = 0;
        long endTime = 0;
        long timeDiff = 0;

        try {
            // set Transaction timeout to 5 seconds
            tm.begin(5);

            // Set start time
            date = new Date();
            startTime = date.getTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy:HH:mm:ss");
            System.out.println("testTransactionTimeoutWithNonAbortableResource Servlet: start the transaction at: " + df.format(date));

            final XAResourceImpl xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo1);

            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2);

            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            // We'll now sit in a long running query for 10 seconds. The query locks up xaRes1 but in the meantime, the
            // transaction will timeout. The query won't be aborted because the XAResource doesn't support that.
            xaRes1.simulateLongRunningQuery(10);

            // Check whether query was aborted
            if (xaRes1.isQueryAborted()) {
                System.out.println("testTransactionTimeoutWithNonAbortableResource XAResource should Not have been aborted<br>");
                throw new Exception("testTransactionTimeoutWithNonAbortableResource XAResource should NOT have been aborted");
            } else {
                // As expected
                System.out.println("testTransactionTimeoutWithNonAbortableResource XAResource was correctly NOT aborted<br>");
            } ;

            date = new Date();
            endTime = date.getTime();
            timeDiff = (endTime - startTime) / 1000;
            String elapsedString = "" + timeDiff;
            System.out.println("testTransactionTimeoutWithNonAbortableResource Servlet: Simulated long query finished at: " + df.format(date) + "after: " + elapsedString);

            // Attempt to Commit the transaction. The timeout will have marked it as rollback only.
            date = new Date();

            System.out.println("testTransactionTimeoutWithNonAbortableResource Servlet: commit the transaction at: " + df.format(date));
            tm.commit();

        } catch (RollbackException rbe) {
            System.out.println("testTransactionTimeoutWithNonAbortableResource Servlet: Caught RollbackException as expected");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.RollbackException" })
    public void testTransactionTimeoutWithAbortableResource(HttpServletRequest request,
                                                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);

        try {
            // set Transaction timeout to 5 seconds
            tm.begin(5);

            final AbortableXAResourceImpl xaRes1 = XAResourceFactoryImpl.instance()
                            .getAbortableXAResourceImpl(xaResInfo1);

            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2);

            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            // We'll now sit in a long running query for 10 seconds. The query locks up xaRes1 but in the meantime, the
            // transaction will timeout so that the query will be aborted.
            xaRes1.simulateLongRunningQuery(10);

            // Check whether query was aborted
            if (xaRes1.isQueryAborted()) {
                // As expected
                System.out.println("testTransactionTimeoutWithAbortableResource XAResource was aborted<br>");
            } else {
                System.out.println("testTransactionTimeoutWithAbortableResource XAResource was NOT aborted<br>");
                throw new Exception("testTransactionTimeoutWithAbortableResource XAResource was NOT aborted");
            } ;
            // Attempt to Commit the transaction. The timeout will have marked it as rollback only.
            Date date = new Date();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy:HH:mm:ss");
            System.out.println("testTransactionTimeoutWithAbortableResource Servlet: commit the transaction at: " + df.format(date));
            tm.commit();

        } catch (RollbackException rbe) {
            System.out.println("testTransactionTimeoutWithAbortableResource Servlet: Caught RollbackException as expected");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}