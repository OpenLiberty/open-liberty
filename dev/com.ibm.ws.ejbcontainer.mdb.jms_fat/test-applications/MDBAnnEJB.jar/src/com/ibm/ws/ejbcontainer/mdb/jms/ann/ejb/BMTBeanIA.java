/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;
import static javax.ejb.TransactionManagementType.BEAN;

import java.security.Principal;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionManagement;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.transaction.NotSupportedException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * This is a BMT Message Driven Bean that is designed to test the container's behavior
 * according to the specification.
 */
// add annotation for activationConfig props
@MessageDriven(description = "This is a description",
               activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "BMTReqQueue")
               },
               name = "BMTBeanIA",
               messageListenerInterface = MessageListener.class //d450391.1
)
@TransactionManagement(BEAN)
public class BMTBeanIA implements DummyInterface {
    private final static String CLASSNAME = BMTBeanIA.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private MessageDrivenContext myMessageDrivenCtx = null;

    //d459309, add authenticationType=APPLICATION
    @Resource(name = "jms/TestQCF", authenticationType = APPLICATION, shareable = true)
    private QueueConnectionFactory replyQueueFactory;

    @Resource(name = "jms/TestResultQueue")
    private Queue replyQueue;

    @EJB
    private SFLocal sfLocal;

    @EJB
    private SF sfR;

    private final String jndiSFL = "java:global/MDBAnnApp/MDBAnnEJB/SFLBean!com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.SFLocal";

    final static String BeanName = "BMTBeanIA";

    public static SFLocal commitBean;
    public static SFLocal rollbackBean;

    String results = null;

    /**
     * This method is a PostConstruct interceptor method that is
     * called when after the Message Driven Bean is created.
     *
     * @throws Exception
     *
     * @exception javax.ejb.CreateException
     */
    @PostConstruct
    private void initialize() {
        svLogger.info(BeanName + "(initialize)");

        // Perform test points of ejbCreate()
        ctx_getCallerPrincipal("ejbCreate()");
        ctx_isCallerInRole("ejbCreate()");
        ctx_getEJBHome("ejbCreate()");
        ctx_getEJBLocalHome("ejbCreate()");
        ctx_getUserTransaction("ejbCreate()");
        ctx_getRollbackOnly("ejbCreate()");
        ctx_setRollbackOnly("ejbCreate()");
    }

    /**
     * This method is a PreDestroy that is called prior to
     * EJB container destroying the MDB.
     */
    @PreDestroy
    private void finish() {
        results = null;
    }

    /**
     * This message is called to inject MessageDrivenContext into bean.
     *
     * @param ctx javax.ejb.MessageDrivenContext
     */
    @Resource
    private void setMessageDrivenContext(MessageDrivenContext ctx) {
        myMessageDrivenCtx = ctx;

        // We set in this method to test that dependency injection
        // does occur prior to PostConstruct interceptors being
        // called by EJB container.  If called in wrong order,
        // the PostContruct method will take a NPE since results is
        // not set (see initialize method).
        results = ""; // d415957
    }

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg This should be a TextMessage.
     * @throws Throwable
     */
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        // Performing test points
        testAssocTxContext("onMessage()");
        ctx_getCallerPrincipal("onMessage()");
        ctx_isCallerInRole("onMessage()");
        ctx_getEJBHome("onMessage()");
        ctx_getEJBLocalHome("onMessage()");
        ctx_getUserTransaction("onMessage()");
        ctx_getRollbackOnly("onMessage()");

        testSFLocal();
        testSFRemote();
        // testSFDS(); //454065
        testBMTTxCommit();
        testBMTTxRollback();

        ctx_testNotSupportedException("onMessage()");
        ctx_setRollbackOnly("onMessage()");

        try {
            // send the result vector through the reply queue
            text = ((TextMessage) msg).getText();

            svLogger.info("senderBean.onMessage(), msg text ->: " + text);

            messageID = msg.getJMSMessageID();
            svLogger.info("Message ID :" + messageID);

            FATMDBHelper.putQueueMessage(results, replyQueueFactory, replyQueue);
            svLogger.info("Test results are sent.");
        } catch (Exception e) {
            svLogger.info("Caught exception: " + e.toString());
            e.printStackTrace();
        }

        return;
    }

    // implementation of onMessage() method from DummyInterface
    @Override
    public void onMessage() {
        testDummyInterface();

        try {
            svLogger.info("In Dummy onMessage()");
            FATMDBHelper.putQueueMessage(results, replyQueueFactory, replyQueue);
            svLogger.info("Test results are sent.");
        } catch (Exception e) {
            svLogger.info("Caught exception: " + e.toString());
            e.printStackTrace();
        }

        return;
    }

    public void testDummyInterface() {
        results = results + " FAIL: onMessage() method from DummyInterface should never be called. ";
    }

    /**
     * To test the access of SLF from MDB
     */
    public void testSFLocal() {
        try {
            String tmpStr = "tmpStr";
            String testStr = sfLocal.method1(tmpStr);
            if (testStr.equals(tmpStr))
                results = results + " SFL.method1 functions correctly. ";
            else
                results = results + " FAIL: SFL.method1 returns a wrong value: " + testStr + ". ";
        } catch (Throwable t) {
            results = results + " FAIL: Caught unexpected: " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    public void testSFRemote() {
        try {
            String tmpStr = "tmpStr";
            String testStr = sfR.method1(tmpStr);
            if (testStr.equals(tmpStr))
                results = results + " SFR.method1 functions correctly. ";
            else
                results = results + " FAIL: SFR.method1 returns a wrong value: " + testStr + ". ";
        } catch (Throwable t) {
            results = results + " FAIL: Caught unexpected: " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    public void testSFDS() {
        try {
            if (sfLocal.getDataSource() == null)
                results = results + " FAIL: DataSource in SFLBean was null ";

            String tmpStr = "teststring";
            String testStr = sfLocal.getStringValue();
            if (testStr.equals(tmpStr))
                results = results + " SFLocal.getStringValue functions correctly. ";
            else
                results = results + " FAIL: SFLocal.getStringValue returns a wrong value: " + testStr + ". ";
        } catch (Throwable t) {
            results = results + " FAIL: Caught unexpected: " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    /**
     * Try to execute getCallerPrincipal()
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getCallerPrincipal(String invokeLoc) {
        try {
            Principal p = myMessageDrivenCtx.getCallerPrincipal();

            if (p == null)
                results = results + " FAIL: getCallerPrincipal() returned null when invoked from " + invokeLoc + ". ";
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception is thrown when " + invokeLoc + " in MDB invokes getCallerPrincipal(): " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    /**
     * Try to execute isCallerInRole()
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_isCallerInRole(String invokeLoc) {
        try {
            boolean b = myMessageDrivenCtx.isCallerInRole("Administrator");
            if ("ejbCreate()".equals(invokeLoc)) {
                results = results + " FAIL: isCallerInRole() cannot be called from ejbCreate. ";
            } else {
                results = results + " isCallerInRole() returns " + b + " from " + invokeLoc + ". ";
            }
        } catch (IllegalStateException ise) {
            if ("ejbCreate()".equals(invokeLoc)) {
                results = results + " isCallerInRole triggers IllegalStateException in MDB's ejbCreate. ";
            } else {
                results = results + " FAIL: IllegalStateException was incorrectly thrown when isCallerInRole was called from " + invokeLoc + ". ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Unexpected exception thrown when isCallerInRole was called from " + invokeLoc + ": " + e.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            e.printStackTrace();
        }
    }

    /**
     * Try to execute getEJBHome()
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getEJBHome(String invokeLoc) {
        try {
            myMessageDrivenCtx.getEJBHome();
            results = results + " FAIL: IllegalStateException should be thrown when getEJBHome is invoked from " + invokeLoc + ". ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " in MDB invokes getEJBHome(). ";
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception is thrown when " + invokeLoc + " in MDB invokes getEJBHome(): " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    /**
     * Try to execute getEJBLocalHome()
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getEJBLocalHome(String invokeLoc) {
        try {
            myMessageDrivenCtx.getEJBLocalHome();
            results = results + " FAIL: IllegalStateException should be thrown when getEJBLocalHome is invoked from " + invokeLoc + ". ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " in MDB invokes getEJBLocalHome(). ";
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception is thrown when " + invokeLoc + " in MDB invokes getEJBLocalHome(): " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    /**
     * Try to execute getRollbackOnly()
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getRollbackOnly(String invokeLoc) {
        try {
            myMessageDrivenCtx.getRollbackOnly();
            results = results + " FAIL: IllegalStateException should be thrown when getRollbackOnly is invoked from " + invokeLoc + " in a BMT MDB. ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " in MDB invokes getRollbackOnly(). ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking getRollbackOnly from " + invokeLoc + " in BMT MDB: " + e.toString() + ". ";
        }
    }

    /**
     * Try to execute setRollbackOnly()
     *
     * @param invokeLoc location MDB is invoked from
     * @throws Exception
     */
    public void ctx_setRollbackOnly(String invokeLoc) {
        try {
            myMessageDrivenCtx.setRollbackOnly();
            results = results + " FAIL: IllegalStateException should be thrown when setRollbackOnly is invoked from " + invokeLoc + ". ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " in MDB invokes setRollbackOnly(). ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking setRollbackOnly from " + invokeLoc + " in BMT MDB: " + e.toString() + ". ";
        }

        try {
            UserTransaction transaction = myMessageDrivenCtx.getUserTransaction();
            transaction.commit();
        } catch (Throwable t) {
            svLogger.info("Exception thrown committing UT: " + t.getMessage());
        }
    }

    /**
     * Try to execute getUserTransaction()
     *
     * @param invokeLoc location MDB is invoked from
     * @throws Exception
     */
    public void ctx_getUserTransaction(String invokeLoc) {
        try {
            UserTransaction transaction = myMessageDrivenCtx.getUserTransaction();
            transaction.getStatus();
            results = results + " A UserTransaction interface can be successfully accessed from " + invokeLoc + " in BMTD MDB. ";
        } catch (Throwable e) {
            results = results + " FAIL: Exception when accessing a UserTransaction interface from " + invokeLoc + " in BMTD MDB: " + e.toString() + ". ";
        }
    }

    /**
     * Call CMP to update the DB and commit the actions
     */
    public void testBMTTxCommit() {
        try {
            svLogger.info(" testBMTTxCommit looking up local bean ...");
            commitBean = (SFLocal) new InitialContext().lookup(jndiSFL);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local for " + jndiSFL + ": " + ne.toString() + ". ";
            return;
        }

        UserTransaction tmpTx = myMessageDrivenCtx.getUserTransaction();
        try {
            tmpTx.begin();

            commitBean.setIntValue(0);
            commitBean.incrementInt();
            tmpTx.commit();
        } catch (Throwable t) {
            results = results + " FAIL: Exception while manipulating SF during commit test: " + t.toString() + ". ";
            return;
        }
    }

    /**
     * Call CMP to update the DB and rollback the actions
     *
     * @throws Throwable
     */
    public void testBMTTxRollback() {
        try {
            svLogger.info(" testBMTTxRollback looking up local bean ...");
            rollbackBean = (SFLocal) new InitialContext().lookup(jndiSFL);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local for " + jndiSFL + ": " + ne.toString() + ". ";
            return;
        }

        UserTransaction tmpTx = myMessageDrivenCtx.getUserTransaction();
        try {
            tmpTx.begin();

            rollbackBean.setIntValue(0);
            rollbackBean.incrementInt();
            tmpTx.rollback();
        } catch (Throwable t) {
            results = results + " FAIL: Exception while manipulating SF during rollback test: " + t.toString() + ". ";
            return;
        }
    }

    /**
     * Get getUserTransaction twice and UserTransaction.begin() before committing the previous transaction
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_testNotSupportedException(String invokeLoc) {
        try {
            UserTransaction transaction1 = myMessageDrivenCtx.getUserTransaction();
            UserTransaction transaction2 = myMessageDrivenCtx.getUserTransaction();
            transaction1.begin();
            transaction2.begin();
            results = results + " FAIL: NotSupportedException should have been thrown. ";
        } catch (Throwable e) {
            if (e instanceof NotSupportedException) {
                svLogger.info("NotSupportedException is generated correctly when " + invokeLoc + " in BMTD MDB.");
                results = results + " NotSupportedException is generated correctly when " + invokeLoc + " in BMTD MDB. ";
            } else {
                results = results + " FAIL:  Unexpected exception is generated when " + invokeLoc + " in BMTD MDB: " + e.toString() + ". ";
            }
        }
    }

    /**
     * To verify there is no transaction context is associated when onMessage() of a BMT MDB is invoked
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void testAssocTxContext(String invokeLoc) {
        try {
            if (!FATTransactionHelper.isTransactionGlobal()) {
                results = results + " The thread is associated to a local transaction. ";
            } else {
                results = results + " FAIL: The thread is associated to a global transaction. ";
            }
        } catch (IllegalStateException ise) {
            results = results + " Correct. No transaction context is associated to the thread. ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected exception thrown in " + invokeLoc + " while testing transaction: " + e.toString() + ". ";
        }
    }
}
