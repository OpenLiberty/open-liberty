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
package com.ibm.tx.jta.ut.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionInflowManager;
import com.ibm.tx.jta.TransactionManagerFactory;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/inflowmanager")
public class InflowManagerTestServlet extends HttpServlet {
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";
    public static final String providerID = "Jon's RA";
    private TransactionInflowManager tim;
    private XATerminator xat;
    private ExtendedTransactionManager tm;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testEC001(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ExecutionContext ec = new ExecutionContext();
        ec.setTransactionTimeout(120);

        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new Exception("Unexpected transaction on thread (1)");
        }

        // In memory of Matt
        XID xid = new XID(180, "MAMWL1".getBytes(), "CAL".getBytes());

        ec.setXid(xid);

        // put transaction on thread
        tim.associate(ec, providerID);

        if (tm.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("No transaction on thread");
        }

        // take transaction off thread
        tim.dissociate();

        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new Exception("Unexpected transaction on thread (2)");
        }

        // commit one phase
        xat.commit(xid, true);
    }

    public void testEC002(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ExecutionContext ec = new ExecutionContext();
        ec.setTransactionTimeout(120);

        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new Exception("Unexpected transaction on thread (1)");
        }

        // In memory of Matt
        XID xid = new XID(180, "MAMWL1".getBytes(), "CAL".getBytes());

        ec.setXid(xid);

        // put transaction on thread
        tim.associate(ec, providerID);

        if (tm.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("No transaction on thread");
        }

        try {
            tim.associate(ec, providerID);
        } catch (WorkCompletedException e) {
            if (!e.getErrorCode().equals(WorkException.TX_RECREATE_FAILED)) {
                throw e;
            }
        }

        // take transaction off thread
        tim.dissociate();

        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new Exception("Unexpected transaction on thread (2)");
        }

        // commit one phase
        xat.commit(xid, true);
    }

    public void testEC003(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ExecutionContext ec = new ExecutionContext();
        ec.setTransactionTimeout(120);

        if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new Exception("Unexpected transaction on thread (1)");
        }

        // In memory of Matt
        XID xid = new XID(180, "MAMWL1".getBytes(), "CAL".getBytes());

        ec.setXid(xid);

        tm.begin();

        if (tm.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("No transaction on thread");
        }

        try {
            // put transaction on thread
            tim.associate(ec, providerID);
        } catch (WorkCompletedException e) {
            if (!e.getErrorCode().equals(WorkException.TX_RECREATE_FAILED)) {
                throw e;
            }
        }

        // commit
        tm.commit();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        BundleContext bc = FrameworkUtil.getBundle(Servlet.class).getBundleContext();

        ServiceTracker<TransactionInflowManager, TransactionInflowManager> st = new ServiceTracker<TransactionInflowManager, TransactionInflowManager>(bc, TransactionInflowManager.class, null);
        st.open();

        ServiceReference<TransactionInflowManager> sr = st.getServiceReference();
        tim = bc.getService(sr);
        xat = tim.getXATerminator(providerID);
        tm = TransactionManagerFactory.getTransactionManager();
    }
}
