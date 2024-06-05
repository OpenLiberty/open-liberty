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
package web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transaction;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/DupXidDBTranlogServlet")
public class DupXidDBTranlogServlet extends FATServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LastingXAResourceImpl.loadState();
        super.doGet(request, response);
    }

    public void setupDupXid(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        try {
            tm.begin();

            final Transaction tx = tm.getTransaction();

            final LastingXAResourceImpl xares1 = XAResourceFactoryImpl.instance().getLastingXAResourceImpl(0);
            xares1.setCommitAction(XAResourceImpl.DIE);
            final LastingXAResourceImpl xares2 = XAResourceFactoryImpl.instance().getLastingXAResourceImpl(1);
            tx.enlistResource(xares1);
            tx.enlistResource(xares2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}