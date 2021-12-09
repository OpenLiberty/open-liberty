/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb;

import java.security.Principal;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.transaction.NotSupportedException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * This is a BMT Message Driven Bean that is designed to test the container's behavior
 * according to the specification.
 */
public class BMTBeanIA implements MessageListener {
    private final static String CLASSNAME = BMTBeanIA.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private MessageDrivenContext myMessageDrivenCtx = null;

    // JNDI names
    private final String replyQueueFactoryName = "java:comp/env/jms/TestQCF";
    private final String replyQueueName = "java:comp/env/jms/TestResultQueue";

    private static final String jndiSFLHome = "java:global/MDBMixApp/MDBMixEJB/MDBSF!com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb.SFLocalHome";
    private static final String jndiSFRHome = "java:global/MDBMixApp/MDBMixEJB/MDBSF!com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb.SFHome";

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

        // Test Point: JNDI
        testJNDIAccess(jndiSFLHome);
    }

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg This should be a TextMessage.
     * @throws Throwable
     */
    @Override
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

            FATMDBHelper.putQueueMessage(results, replyQueueFactoryName, replyQueueName);
            svLogger.info("Test results are sent.");
        } catch (Exception e) {
            svLogger.info("Caught exception: " + e.toString());
            e.printStackTrace();
        }

        return;
    }

    /**
     * To test the access of SLF from MDB
     */
    public void testSFLocal() {
        SFLocalHome fhome1;

        try {
            fhome1 = (SFLocalHome) FATHelper.lookupJavaBinding(jndiSFLHome);
            if (fhome1 == null) {
                results = results + " FAIL: Unable to lookup bean home for " + jndiSFLHome + ". ";
                return;
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception thrown when looking up " + jndiSFLHome + ": " + e.toString() + ". ";
            svLogger.info("Lookup Exception:");
            e.printStackTrace();
            return;
        }

        SFLocal ejb1 = null;

        try {
            ejb1 = fhome1.create();
            if (ejb1 == null) {
                results = results + " FAIL: Couldn't create SFL from the Home interface. ";
            } else {
                results = results + " SFL created from the Home interface successfully. ";

                String tmpStr = "tmpStr";
                String testStr = ejb1.method1(tmpStr);
                if (testStr.equals(tmpStr))
                    results = results + " SFL.method1 functions correctly. ";
                else
                    results = results + " FAIL: SFL.method1 returns a wrong value: " + testStr + ". ";
            }
        } catch (Throwable t) {
            results = results + " FAIL: Caught unexpected: " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        } finally {
            if (ejb1 != null) {
                try {
                    ejb1.remove();
                    svLogger.info("Cleanup completed, EJB removed");
                } catch (Throwable t) {
                    svLogger.info("Warning: Unable to remove EJB.");
                }
            }
        }
    }

    public void testSFRemote() {
        SFHome fhome1;

        try {
            fhome1 = FATHelper.lookupRemoteBinding(jndiSFRHome, SFHome.class);
            if (fhome1 == null) {
                results = results + " FAIL: Unable to lookup bean home for " + jndiSFRHome + ". ";
                return;
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception thrown when looking up " + jndiSFRHome + ": " + e.toString() + ". ";
            svLogger.info("Lookup Exception:");
            e.printStackTrace();
            return;
        }

        SF ejb1 = null;

        try {
            ejb1 = fhome1.create();
            if (ejb1 == null) {
                results = results + " FAIL: Couldn't create SF remote object from the remote home interface. ";
            } else {
                results = results + " SF remote object created from the remote home interface successfully. ";

                String tmpStr = "tmpStr";
                String testStr = ejb1.method1(tmpStr);
                if (testStr.equals(tmpStr))
                    results = results + " method1() of SF remote object functions correctly. ";
                else
                    results = results + " FAIL: SF.method1 returns a wrong value: " + testStr + ". ";
            }
        } catch (Throwable t) {
            results = results + " FAIL: Caught unexpected: " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        } finally {
            if (ejb1 != null) {
                try {
                    ejb1.remove();
                    svLogger.info("Cleanup completed, EJB removed");
                } catch (Throwable t) {
                    svLogger.info("Warning: Unable to remove EJB.");
                }
            }
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
     * Access SF Local interface from MDB
     *
     * @param jndiName JNDI Name of the SF Local home
     */
    public void testJNDIAccess(String jndiName) {
        try {
            SFLocalHome fhome1 = (SFLocalHome) FATHelper.lookupJavaBinding(jndiSFLHome);
            if (fhome1 == null) {
                results = results + " FAIL: JNDI resource is null. ";
            } else {
                results = results + " The JNDI resource is found and not null. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing JNDI: " + e.toString() + ". ";
        }
    }

    /**
     * Call CMP to update the DB and commit the actions
     */
    public void testBMTTxCommit() {
        SFLocalHome fhome1;

        try {
            svLogger.info(" testBMTTxCommit looking up local home ...");
            fhome1 = (SFLocalHome) new InitialContext().lookup(jndiSFLHome);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local home for " + jndiSFLHome + ": " + ne.toString() + ". ";
            return;
        }

        UserTransaction tmpTx = myMessageDrivenCtx.getUserTransaction();
        try {
            tmpTx.begin();

            svLogger.info("create - started.");
            commitBean = fhome1.create();
            svLogger.info("create - ended.");
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
        SFLocalHome fhome1;

        try {
            svLogger.info(" testBMTTxRollback looking up local home ...");
            fhome1 = (SFLocalHome) new InitialContext().lookup(jndiSFLHome);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local home for " + jndiSFLHome + ": " + ne.toString() + ". ";
            return;
        }

        UserTransaction tmpTx = myMessageDrivenCtx.getUserTransaction();
        try {
            tmpTx.begin();

            svLogger.info("create - started.");
            rollbackBean = fhome1.create();
            svLogger.info("create - ended.");
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
