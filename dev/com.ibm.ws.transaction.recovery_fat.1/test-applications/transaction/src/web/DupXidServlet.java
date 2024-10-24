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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/DupXidServlet")
public class DupXidServlet extends FATServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        XAResourceImpl.setStateFile(new File(System.getenv("WLP_OUTPUT_DIR") + "/../shared/" + LastingXAResourceImpl.STATE_FILE_ROOT));
        XAResourceImpl.loadState();
        super.doGet(request, response);
    }

    public void setupDupXid(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}