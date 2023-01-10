/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package test.jca.jms.example.mdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.sql.DataSource;

@MessageDriven
public class InventoryTrackerMDB implements MessageListener {
    @Resource(lookup = "concurrent/execSvc1")
    private ExecutorService execSvc1;

    /**
     * Listens for "low inventory" notifications from the JCAStoreServlet.
     */
    @Override
    public void onMessage(Message message) {
        System.out.println("> onMessage: " + message);
        try {
            final String lowInventoryItem = ((TextMessage) message).getText();

            // On one thread, consult the sales report to see how many have been sold in total
            Future<Integer> totalSold = execSvc1.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/jdbc/derby");
                    Connection con = ds.getConnection();
                    try {
                        PreparedStatement pstmt = con.prepareStatement("select sum(QUANTITY) from SALESREPORT where ITEM=?");
                        pstmt.setString(1, lowInventoryItem);
                        ResultSet result = pstmt.executeQuery();
                        return result.next() ? result.getInt(1) : 0;
                    } finally {
                        con.close();
                    }
                }
            });

            // On another thread, consult the sales projections to see how many we expect to sell
            Future<Integer> projection = execSvc1.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/jdbc/derby");
                    Connection con = ds.getConnection();
                    try {
                        PreparedStatement pstmt = con.prepareStatement("select QUANTITY from SALESPROJECTIONS where ITEM=?");
                        pstmt.setString(1, lowInventoryItem);
                        ResultSet result = pstmt.executeQuery();
                        result.next();
                        return result.getInt(1);
                    } finally {
                        con.close();
                    }
                }
            });

            // Make up some fake business logic to decide how many more to produce to replenish the low inventory item
            int amountToProduce;
            if (totalSold.get() > projection.get())
                amountToProduce = projection.get() / 5;
            else
                amountToProduce = projection.get() / 10;

            // Then pretend like we can instantly produce them and add them to the inventory
            DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/jdbc/derby");
            Connection con = ds.getConnection();
            try {
                PreparedStatement pstmt = con.prepareStatement("update PRODUCTS set AVAILABLE=AVAILABLE+? where ITEM=?");
                pstmt.setInt(1, amountToProduce);
                pstmt.setString(2, lowInventoryItem);
                pstmt.executeUpdate();
            } finally {
                con.close();
            }

            System.out.println("InventoryTrackerMDB completed processing for item " + lowInventoryItem + ". Amount added: " + amountToProduce);
            System.out.println("< onMessage: " + lowInventoryItem + ": add " + amountToProduce);
        } catch (Exception x) {
            System.out.println("< onMessage: exception");
            x.printStackTrace(System.out);
            throw new RuntimeException(x);
        }
    }
}
