/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.transaction.web;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SSLRecoveryServlet")
public class SSLRecoveryServlet extends FATServlet {

    @Resource
    UserTransaction ut;

    @Resource(name = "jdbc/anonymous/XADataSource")
    DataSource ds;

    public void setupRec001(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);

        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        ut.begin();

        final XAResource xaRes1 = ((XAResourceImpl) (XAResourceFactoryImpl.instance()
                        .getXAResource(xaResInfo1))).setCommitAction(XAResourceImpl.DIE);

        final int recoveryId1 = tm.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo1);

        tm.enlist(xaRes1, recoveryId1);

        // Sometimes the SSL config is not ready. We'll keep trying.
        while (true) {
            try (Connection con = ds.getConnection()) {
                con.createStatement().execute("INSERT INTO people(id,name) VALUES(1,'Jon')");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(5000);
            }
        }

        ut.commit();
    }

    public void checkRec001(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {

        try (Connection con = ds.getConnection()) {
            if (XAResourceImpl.resourceCount() != 1) {
                throw new Exception("Rec001 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec001 failed: Dummy resource not recovered");
            }

            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM people WHERE id=1");

            if (!rs.next()) {
                throw new Exception("Rec001 failed: Not committed");
            }
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void testBothRollback(HttpServletRequest request,
                                 HttpServletResponse response) throws SQLException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {

        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);

            ut.begin();

            try (PreparedStatement ps1 = con.prepareStatement("insert into people values (?, ?)");
                            PreparedStatement ps2 = con.prepareStatement("insert into XXXXXX values (?, ?)")) {
                ps1.setInt(1, 17);
                ps1.setString(2, "Seventeen");
                ps2.setInt(1, 37);
                ps2.setString(2, "Thirty-Seven");

                con.setAutoCommit(false);

                ps1.executeUpdate();
                // This next statement is destined to fail (because of the XXXXXX table name in the insert statement),
                // throwing a SQLException which will ultimately result in the transaction rolling back.
                ps2.executeUpdate();

            }

            // This should never be reached
            ut.commit();
        }
    }
}