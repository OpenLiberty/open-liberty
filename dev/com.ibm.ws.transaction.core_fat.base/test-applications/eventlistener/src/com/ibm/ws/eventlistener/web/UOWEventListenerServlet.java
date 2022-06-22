/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.eventlistener.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.fat.eventListener.TestUOWEventListener;
import com.ibm.wsspi.tx.UOWEventListener;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class
 */
@SuppressWarnings("serial")
@WebServlet("/UOWEventListenerServlet")
public class UOWEventListenerServlet extends FATServlet {

    @Inject
    private TransactionalBean txBean;

    @Resource
    UserTransaction ut;

    @Resource(name = "jdbc/derby")
    private DataSource ds;

    // Lots of scope to do piles of LTC testing here in future

    @Test
    public void testBasicOperation() throws Exception {
        TestUOWEventListener.start();

        txBean.required();

        TestUOWEventListener.check(new Integer[] { UOWEventListener.POST_BEGIN, UOWEventListener.SUSPEND, UOWEventListener.POST_END });

    }

    /**
     * Test of basic database connectivity
     */
    @Test
    public void testBasicConnection() throws Exception {
        TestUOWEventListener.start();

        Connection con = ds.getConnection();

        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");

            ut.begin();
            try {
                stmt = con.createStatement();
                stmt.executeUpdate("update bvtable set col1=24, col2='XXIV' where col1=13");
            } finally {
                ut.commit();
            }
        } finally {
            con.close();
        }

        TestUOWEventListener.check(new Integer[] { UOWEventListener.POST_BEGIN, UOWEventListener.REGISTER_SYNC, UOWEventListener.SUSPEND, UOWEventListener.POST_END });
    }
}