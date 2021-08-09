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

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import org.junit.Test;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SSLRecoveryServlet")
public class SSLRecoveryServlet extends FATServlet {

  @Resource
  UserTransaction ut;

  private static final String filter = "(testfilter=jon)";

  public void setupRec001(HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
    final Serializable xaResInfo1 = XAResourceInfoFactory
                    .getXAResourceInfo(0);

    final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

    ut.begin();

    final XAResource xaRes1 = ((XAResourceImpl)(XAResourceFactoryImpl.instance()
                        .getXAResource(xaResInfo1))).setCommitAction(XAResourceImpl.DIE);

    final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);

    tm.enlist(xaRes1, recoveryId1);

    DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");

    try (Connection con = ds.getConnection()) {
        con.createStatement().execute("INSERT INTO people(id,name) VALUES(1,'Jon')");
    }

    ut.commit();
  }

  public void checkRec001(HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
    DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");
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
}