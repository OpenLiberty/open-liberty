/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.msgendpoint.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkEvent;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.CMTRollback;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.XidImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

import componenttest.app.FATServlet;

/**
 * Test Name: MsgEndpoint_TXTest
 *
 * Test Descriptions:
 *
 * This test class is to cover scenarios using imported transaction.
 *
 * Test Matrix:
 * <ol>
 * <li>Testing message delivery options
 * <li>XAResource Delivery Option Imported Tx
 * <li>testXAOptionACommit: Yes A Commit
 * <li>testNonXAOptionACommit: No A Commit
 * <li>testXAOptionBCommit: Yes B Commit
 * <li>testNonXAOptionBCommit: No B Commit
 * <li>testXAOptionARollback: Yes A Rollback
 * <li>testNonXAOptionARollback: No A Rollback
 * <li>testXAOptionBRollback: Yes B Rollback
 * <li>testNonXAOptionBRollback: No B Rollback
 * <li>testXAOptionAMDBRollback
 * <li>testNonXAOptionAMDBRollback
 * <li>testXAOptionBMDBRollback
 * <li>testNonXAOptionBMDBRollback
 * </ol>
 */
public class MsgEndpoint_TXServlet extends FATServlet {
    @Resource(name = "jdbc/FAT_TRA_DS", shareable = true)
    private DataSource ds1;

    private FVTMessageProvider provider = null;

    private static final String keyCommit01 = "TX_Commit01";
    private static final String keyCommit02 = "TX_Commit02";
    private static final String keyCommit03 = "TX_Commit03";
    private static final String keyCommit04 = "TX_Commit04";

    private static final String keyMDBRollback01 = "TX_MDBRollback01";
    private static final String keyMDBRollback02 = "TX_MDBRollback02";
    private static final String keyMDBRollback03 = "TX_MDBRollback03";
    private static final String keyMDBRollback04 = "TX_MDBRollback04";

    private static final String keySourceRollback01 = "TX_SourceRollback01";
    private static final String keySourceRollback02 = "TX_SourceRollback02";
    private static final String keySourceRollback03 = "TX_SourceRollback03";
    private static final String keySourceRollback04 = "TX_SourceRollback04";

    public void prepareTRA() throws Exception {
        provider = (FVTMessageProvider) new InitialContext().lookup("java:comp/env/MessageProvider");
        System.out.println("Looked up MessageProvider");
        provider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        Connection conn = null;
        Statement statement = null;

        try {
            conn = ds1.getConnection();
            statement = conn.createStatement();
            statement.execute("create table msg (pkey varchar(250) not null primary key, intvalue integer not null, stringvalue varchar(250))");
            statement.close();
        } catch (SQLException e) {
            // Let a table setup failure be thrown in the test, faster to debug
            // that way
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }

        insertData(keyCommit01, 0);
        insertData(keyCommit02, 0);
        insertData(keyCommit03, 0);
        insertData(keyCommit04, 0);
        insertData(keyMDBRollback01, 0);
        insertData(keyMDBRollback02, 0);
        insertData(keyMDBRollback03, 0);
        insertData(keyMDBRollback04, 0);
        insertData(keySourceRollback01, 0);
        insertData(keySourceRollback02, 0);
        insertData(keySourceRollback03, 0);
        insertData(keySourceRollback04, 0);
    }

    public void insertData(String pKey, int intValue) {
        Connection conn = null;
        Statement statement = null;

        try {
            conn = ds1.getConnection();
            statement = conn.createStatement();
            statement.execute("insert into msg values('" + pKey + "'," + intValue + ",'SINGLEDATA')");
            statement.close();
            System.out.println("Insert into key " + pKey + " successful");
        } catch (SQLException e) {
            System.out.println("Exception inserting into key " + pKey + ": " + e);
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }
    }

    public void resetData(String key) throws Exception {
        Connection conn = null;
        Statement statement = null;

        try {
            conn = ds1.getConnection();
            statement = conn.createStatement();
            statement.execute("update msg set intvalue=0 where pkey='" + key + "'");
            System.out.println("Reset pKey " + key + " to 0");
            statement.close();
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public int verifyData(String pKey) throws Exception {
        Connection conn = null;
        Statement statement = null;
        ResultSet rs = null;
        int intValue = 0;

        try {
            conn = ds1.getConnection();
            statement = conn.createStatement();
            rs = statement.executeQuery("select intvalue from msg where pkey = '" + pKey + "'");
            System.out.println("Got result in verify: " + rs);
            rs.next();
            intValue = rs.getInt(1);
            System.out.println("intvalue is " + intValue);
            statement.close();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }

        return intValue;
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A global transaction is imported
     * and used for the option A message delivery.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that XAResource provided by RA is neither
     * enlisted into the global transaction nor is commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionACommit() throws Exception {
        prepareTRA();

        resetData(keyCommit01);

        String deliveryID = "TX_test1a";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 201);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 201, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "DB-" + keyCommit01, m, 201);
        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(11, 10); // d248457.1
        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource is not enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify Commit01
        assertTrue(keyCommit01 + " -- intValue is updated.", (verifyData(keyCommit01) == 1000));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A global transaction is imported
     * and used for the option A message delivery.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNonXAOptionACommit() throws Exception {
        prepareTRA();

        resetData(keyCommit02);

        String deliveryID = "TX_test1b";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "DB-" + keyCommit02, m);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(12, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("No RA XAResource is enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify Commit02
        assertTrue(keyCommit02 + " -- intValue is updated.", (verifyData(keyCommit02) == 1000));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A global transaction is imported
     * and used for the message delivery.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionBCommit() throws Exception {
        prepareTRA();

        resetData(keyCommit03);

        String deliveryID = "TX_test1c";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 203);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 203, new FVTXAResourceImpl());

        // Add a option B delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 203);
        message.add("CMTNonJMSRequired", "DB-" + keyCommit03, m, 203);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 203);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(13, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource is not enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify Commit03
        assertTrue(keyCommit03 + " -- intValue is updated.", (verifyData(keyCommit03) == 1000));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Required</i> transaction attribute. A global transaction is imported
     * and used for the message delivery.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testNonXAOptionBCommit() throws Exception {
        prepareTRA();

        resetData(keyCommit04);

        String deliveryID = "TX_test1d";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSRequired", "DB-" + keyCommit04, m);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(14, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource is not enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify Commit04
        assertTrue(keyCommit04 + " -- intValue is updated.", (verifyData(keyCommit04) == 1000));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the imported transaction is rolled back.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionARollback() throws Exception {
        prepareTRA();

        resetData(keyMDBRollback01);

        String deliveryID = "TX_test2a";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 211);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 211, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "DB-" + keyMDBRollback01, m, 211);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(21, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        XATerminator xaTerm = provider.getXATerminator();
        xaTerm.rollback(xid);

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID); // ???
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keyMDBRollback01
        assertTrue(keyMDBRollback01 + " -- intValue is not updated.", (verifyData(keyMDBRollback01) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the imported transaction is rolled back.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testNonXAOptionARollback() throws Exception {
        prepareTRA();

        resetData(keyMDBRollback02);

        String deliveryID = "TX_test2b";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "DB-" + keyMDBRollback02, m);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(22, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        xaTerm.rollback(xid);

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID); // ???
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keyMDBRollback02
        assertTrue(keyMDBRollback02 + " -- intValue is not updated.", (verifyData(keyMDBRollback02) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the imported transaction is rolled back.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionBRollback() throws Exception {
        prepareTRA();

        resetData(keyMDBRollback03);

        String deliveryID = "TX_test2c";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 213);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 213, new FVTXAResourceImpl());

        // Add a option B delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 213);
        message.add("CMTNonJMSRequired", "DB-" + keyMDBRollback03, m, 213);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 213);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(23, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        XATerminator xaTerm = provider.getXATerminator();
        xaTerm.rollback(xid);

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keyMDBRollback03
        assertTrue(keyMDBRollback03 + " -- intValue is not updated.", (verifyData(keyMDBRollback03) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the imported transaction is rolled back.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testNonXAOptionBRollback() throws Exception {
        prepareTRA();

        resetData(keyMDBRollback04);

        String deliveryID = "TX_test2d";

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSRequired", "DB-" + keyMDBRollback04, m);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(24, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        XATerminator xaTerm = provider.getXATerminator();
        xaTerm.rollback(xid);

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keyMDBRollback04
        assertTrue(keyMDBRollback04 + " -- intValue is not updated.", (verifyData(keyMDBRollback04) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the MDB method is to rollback a designated times
     * until it finally commits.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionAMDBRollback() throws Exception {
        prepareTRA();

        resetData(keySourceRollback01);

        String deliveryID = "TX_test3a";

        CMTRollback.mdbInvokedTimes = 0;

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTRollbackRequired", 221);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTRollbackRequired", 221, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTRollbackRequired", "DB-" + keySourceRollback01, m, 221);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(31, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        try {
            if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
                // Now commit xid
                xaTerm.commit(xid, false);
            }

            fail("Global tran should be rolled back by the MDB.");

        } catch (XAException xae) {
            // xaTerm.rollback(xid);
            System.out.println("XAException.errorCode is " + xae.errorCode);
            System.out.println("Global tran is rolled back: " + xae);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID); // ???
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keySourceRollback01
        assertTrue(keySourceRollback01 + " -- intValue is not updated.", (verifyData(keySourceRollback01) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the MDB method is to rollback a designated times
     * until it finally commits.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testNonXAOptionAMDBRollback() throws Exception {
        prepareTRA();

        resetData(keySourceRollback02);

        String deliveryID = "TX_test3b";

        CMTRollback.mdbInvokedTimes = 0;

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTRollbackRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTRollbackRequired", "DB-" + keySourceRollback02, m);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(32, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();

        try {
            if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
                // Now commit xid
                xaTerm.commit(xid, false);
            }

            fail("Global tran should be rolled back by the MDB.");

        } catch (XAException xae) {
            // xaTerm.rollback(xid);
            System.out.println("XAException.errorCode is " + xae.errorCode);
            System.out.println("Global tran is rolled back: " + xae);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID); // ???
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keySourceRollback02
        assertTrue(keySourceRollback02 + " -- intValue is not updated.", (verifyData(keySourceRollback02) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the MDB method is to rollback a designated times
     * until it finally commits.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testXAOptionBMDBRollback() throws Exception {
        prepareTRA();

        resetData(keySourceRollback03);

        String deliveryID = "TX_test3c";

        CMTRollback.mdbInvokedTimes = 0;

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTRollbackRequired", 223);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTRollbackRequired", 223, new FVTXAResourceImpl());

        // Add a option B delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTRollbackRequired", FVTMessage.BEFORE_DELIVERY, m, 223);
        message.add("CMTRollbackRequired", "DB-" + keySourceRollback03, m, 223);
        message.addDelivery("CMTRollbackRequired", FVTMessage.AFTER_DELIVERY, null, 223);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(33, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        try {
            if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
                // Now commit xid
                xaTerm.commit(xid, false);
            }

            fail("Global tran should be rolled back by the MDB.");

        } catch (XAException xae) {
            // xaTerm.rollback(xid);
            System.out.println("XAException.errorCode is " + xae.errorCode);
            System.out.println("Global tran is rolled back: " + xae);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keySourceRollback03
        assertTrue(keySourceRollback03 + " -- intValue is not updated.", (verifyData(keySourceRollback03) == 0));
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Required</i> transaction attribute. A imported global transaction
     * is used to deliver message and the MDB method is to rollback a designated times
     * until it finally commits.
     *
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery, XAResource provided by RA is not enlisted
     * into the global transaction, and neither commit nor rollback is driven on the XAResource.
     */
    public void testNonXAOptionBMDBRollback() throws Exception {
        prepareTRA();

        resetData(keySourceRollback04);

        String deliveryID = "TX_test3d";

        CMTRollback.mdbInvokedTimes = 0;

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTRollbackRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });

        message.addDelivery("CMTRollbackRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTRollbackRequired", "DB-" + keySourceRollback04, m);
        message.addDelivery("CMTRollbackRequired", FVTMessage.AFTER_DELIVERY, null);

        System.out.println(message.toString());
        XidImpl xid = XidImpl.createXid(34, 10); // d248457.1

        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();

        try {
            if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) {
                // Now commit xid
                xaTerm.commit(xid, false);
            }

            fail("Global tran should be rolled back by the MDB.");

        } catch (XAException xae) {
            // xaTerm.rollback(xid);
            System.out.println("XAException.errorCode is " + xae.errorCode);
            System.out.println("Global tran is rolled back: " + xae);
        }

        // Get the first test result array for the endpoint instance 0
        MessageEndpointTestResults results = provider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
        assertFalse("commit is not driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("rollback is not driven on the XAResource provided by TRA.", results.raXaResourceRollbackWasDriven());

        provider.releaseDeliveryId(deliveryID);

        // verify keySourceRollback04
        assertTrue(keySourceRollback04 + " -- intValue is not updated.", (verifyData(keySourceRollback04) == 0));
    }
}
