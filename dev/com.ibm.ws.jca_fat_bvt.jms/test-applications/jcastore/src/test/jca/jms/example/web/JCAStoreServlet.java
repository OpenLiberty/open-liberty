/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.jms.example.web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class JCAStoreServlet extends FATServlet {
    @Resource(lookup = "jdbc/derby", name = "java:module/env/jdbc/derby")
    DataSource dataSource;

    @Resource(lookup = "jms/LowInventoryQueue")
    private Queue lowInventoryNotificationQueue;

    @Resource(lookup = "jms/qcf")
    private QueueConnectionFactory qcf;

    @Override
    public void init() throws ServletException {
        //Create table in database container
        try (Connection con = dataSource.getConnection()) {
            try (Statement st = con.createStatement()) {
                populateDatabase(st);
            }
        } catch (SQLException e) {
            //Ignore, server could have been restarted. Assume table is available.
        }

    }

    private void populateDatabase(Statement stmt) throws SQLException {
        // clear any old data
        for (String table : new String[] { "PRODUCTS", "SALESREPORT", "SALESPROJECTIONS" })
            try {
                stmt.executeUpdate("drop table " + table);
            } catch (SQLException x) {
            }
        // create & populate tables
        stmt.executeUpdate("create table PRODUCTS (ITEM varchar(50) not null primary key, PRICE float, AVAILABLE int)");
        stmt.executeUpdate("create table SALESREPORT (ITEM varchar(50), QUANTITY int)");
        stmt.executeUpdate("create table SALESPROJECTIONS (ITEM varchar(50) not null primary key, QUANTITY int)");
        stmt.addBatch("insert into PRODUCTS values ('Batteries', 9.99, 51)");
        stmt.addBatch("insert into PRODUCTS values ('Lightbulbs', 4.99, 27)");
        stmt.addBatch("insert into PRODUCTS values ('Stapler', 2.99, 12)");
        stmt.addBatch("insert into PRODUCTS values ('Sunglasses', 3.99, 15)");
        stmt.addBatch("insert into PRODUCTS values ('Toothbrush', 0.99, 30)");
        stmt.addBatch("insert into SALESPROJECTIONS values ('Batteries', 1000)");
        stmt.addBatch("insert into SALESPROJECTIONS values ('Lightbulbs', 300)");
        stmt.addBatch("insert into SALESPROJECTIONS values ('Stapler', 70)");
        stmt.addBatch("insert into SALESPROJECTIONS values ('Sunglasses', 100)");
        stmt.addBatch("insert into SALESPROJECTIONS values ('Toothbrush', 200)");
        stmt.executeBatch();
    }

    public void purchase10Lightbulbs() throws Exception {
        //Create table in database container
        try (Connection con = dataSource.getConnection()) {
            try (Statement st = con.createStatement()) {
                purchaseItems(st, "Lightbulbs", 10);
            }
        }
    }

    public void purchase12Lightbulbs() throws Exception {
        //Create table in database container
        try (Connection con = dataSource.getConnection()) {
            try (Statement st = con.createStatement()) {
                purchaseItems(st, "Lightbulbs", 12);
            }
        }
    }

    public void purchase32Lightbulbs() throws Exception {
        //Create table in database container
        try (Connection con = dataSource.getConnection()) {
            try (Statement st = con.createStatement()) {
                purchaseItems(st, "Lightbulbs", 32);
            }
        }
    }

    private void purchaseItems(Statement stmt, String item, int quantity) throws JMSException, SQLException {
        // Update the product inventory database and sales report
        boolean committed = false;
        Connection con = stmt.getConnection();
        con.setAutoCommit(false);
        try {
            if (quantity > 0) {
                int numUpdates = stmt.executeUpdate("update PRODUCTS set AVAILABLE=AVAILABLE-" + quantity + " where ITEM='" + item + "' and AVAILABLE>=" + quantity);
                if (numUpdates != 1)
                    throw new RuntimeException("Unable to fulfill order for " + quantity + " of item type " + item);
                stmt.executeUpdate("insert into SALESREPORT values ('" + item + "', " + quantity + ")");
            }
            con.commit();
            committed = true;
        } finally {
            if (!committed)
                con.rollback();
            con.setAutoCommit(true);
        }

        // Send out notifications when the amount in stock of any item drops below 10
        ResultSet lowInventory = stmt.executeQuery("select ITEM from PRODUCTS where AVAILABLE < 10");
        if (lowInventory.next()) {
            QueueConnection qcon = qcf.createQueueConnection();
            try {
                QueueSession session = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                QueueSender sender = session.createSender(lowInventoryNotificationQueue);
                do
                    sender.send(session.createTextMessage(lowInventory.getString("ITEM")));
                while (lowInventory.next());
            } finally {
                qcon.close();
            }
        }
        lowInventory.close();
    }

}