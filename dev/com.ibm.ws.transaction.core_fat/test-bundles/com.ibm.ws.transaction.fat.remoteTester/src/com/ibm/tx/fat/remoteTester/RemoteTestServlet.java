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
package com.ibm.tx.fat.remoteTester;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;
import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.tx.remote.Vote;

/**
 * Servlet implementation class
 */
@WebServlet("/remoteTester")
public class RemoteTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

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

    @Resource
    UserTransaction ut;

    private static RemoteTransactionController _rtc;

    private static TransactionManager _tm;

    public static void setRemoteTransactionController(RemoteTransactionController rtc) {
        System.out.println("setRemoteTransactionController");
        _rtc = rtc;
    }

    public void testBasicOperation(HttpServletRequest request, HttpServletResponse response) throws Exception {
    }

    public void testExport(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String globalId = _rtc.exportTransaction();
            throw new Exception("Didn't catch SystemException");
        } catch (SystemException e) {
            // success. there's no transaction
        }

        ut.begin();
        String globalId = _rtc.exportTransaction();
        System.out.println("Exporting transaction: " + globalId);

        _rtc.unexportTransaction(globalId);
    }

    public void testRegisterParticipant(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ut.begin();

        String globalId = _rtc.exportTransaction();

        if (!_rtc.registerRemoteParticipant("(testfilter=wsat)", XAResourceInfoFactory.getXAResourceInfo(1), globalId)) {
            throw new Exception("registerRemoteParticipant returned false");
        }

        _rtc.unexportTransaction(globalId);

        ut.commit();
    }

    public void testImport(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final String globalId = "testImport";

        final boolean createdTransaction = _rtc.importTransaction(globalId, 129);

        if (!createdTransaction) {
            throw new Exception("Import didn't create transaction");
        }

        if (ut.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("Imported transaction is not active");
        }

        _rtc.unimportTransaction(globalId);
    }

    public void testBasicCompletion(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final String globalId = "testBasicCompletion";

        final boolean createdTransaction = _rtc.importTransaction(globalId, 129);

        if (!createdTransaction) {
            throw new Exception("Import didn't create transaction");
        }

        if (ut.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("Imported transaction is not active");
        }

        _rtc.unimportTransaction(globalId);

        Vote vote = _rtc.prepare(globalId);

        if (vote != Vote.VoteReadOnly) {
            throw new Exception("Imported transaction did not vote readonly");
        }
    }

    public void testCompleteResources(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final String globalId = "testCompleteResources";

        final boolean createdTransaction = _rtc.importTransaction(globalId, 129);

        if (!createdTransaction) {
            throw new Exception("Import didn't create transaction");
        }

        if (ut.getStatus() != Status.STATUS_ACTIVE) {
            throw new Exception("Imported transaction is not active");
        }

        final Transaction tx = _tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        _rtc.unimportTransaction(globalId);

        final Vote vote = _rtc.prepare(globalId);

        System.out.println("Prepare voted: " + vote);

        if (vote != Vote.VoteCommit) {
            throw new Exception("Imported transaction did not vote commit");
        }

        _rtc.commit(globalId);
    }

    public void setupRecBasicRootRecovery(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ut.begin();

        String globalId = _rtc.getGlobalId();

        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);;
        final int recoveryId1 = ((ExtendedTransactionManager) _tm).registerResourceInfo("(testfilter=jon)", xaResInfo1);
        ((ExtendedTransactionManager) _tm).enlist(xaRes1, recoveryId1);

        if (!_rtc.registerRemoteParticipant("(testfilter=wsat2)", XAResourceInfoFactory.getXAResourceInfo(1), globalId)) {
            throw new Exception("registerRemoteParticipant returned false");
        }

        _rtc.exportTransaction();

        // Between export & unexport, the tran is off the thread

        if (_tm.getStatus() != Status.STATUS_NO_TRANSACTION)
            throw new Exception("Exported transaction was not suspended (" + _tm.getStatus() + ")");

        _rtc.unexportTransaction(globalId);

        ut.commit();
    }

    public void checkRecBasicRootRecovery(HttpServletRequest request, HttpServletResponse response) throws Exception {
    }

    /**
     * @param tm
     */
    public static void setTransactionManager(TransactionManager tm) {
        _tm = tm;
    }
}