/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.ejb20;

import java.security.Principal;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a CMT Message Driven Bean that is implemented to test the container's behaviors
 * according to the EJB specification
 */
@SuppressWarnings("serial")
public class CMTBeanIA implements MessageDrivenBean, MessageListener {
    private final static String CLASSNAME = CMTBeanIA.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private javax.ejb.MessageDrivenContext myMessageDrivenCtx = null;

    // JNDI for result queue
    private final String replyQueueFactoryName = "jms/TestQCF";
    private final String replyQueueName = "jms/TestResultQueue";

    // JNDI for session beans
    private final String ejbJndiName1 = "java:global/MDB20TestApp/MDB20TestEJB/MDBSF!com.ibm.ws.ejbcontainer.mdb.jms.ejb20.SFLocalHome";
    private static final String ejbJndiName2 = "java:global/MDB20TestApp/MDB20TestEJB/MDBSLL!com.ibm.ws.ejbcontainer.mdb.jms.ejb20.SLLaHome";

    // MDB name
    final static String BeanName = "CMTBeanIA";

    //Test points
    String results = "";

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() throws CreateException {
        // Perform test points of ejbCreate()
        ctx_getCallerPrincipal("ejbCreate()");
        ctx_isCallerInRole("ejbCreate()");
        ctx_getEJBHome("ejbCreate()");
        ctx_getEJBLocalHome("ejbCreate()");
        ctx_getUserTransaction("ejbCreate()");
        ctx_getRollbackOnlyFailed("ejbCreate()");
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    @Override
    public void ejbRemove() {
    }

    /**
     * This method returns the MessageDrivenContext for this Message Driven Bean. The object returned
     * is the same object that is passed in when setMessageDrivenContext is called
     *
     * @return javax.ejb.MessageDrivenContext
     */
    public MessageDrivenContext getMessageDrivenContext() {
        return myMessageDrivenCtx;
    }

    /**
     * This message stores the MessageDrivenContext in case it is needed later, or the getMessageDrivenContext
     * method is called.
     *
     * @param ctx javax.ejb.MessageDrivenContext
     * @exception javax.ejb.EJBException The exception description.
     */
    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        myMessageDrivenCtx = ctx;

        // Test Point: JNDI
        testJNDIAccess(ejbJndiName1);
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
        ctx_getCallerPrincipal("onMessage()");
        ctx_isCallerInRole("onMessage()");
        ctx_getEJBHome("onMessage()");
        ctx_getEJBLocalHome("onMessage()");
        ctx_getUserTransaction("onMessage()");
        ctx_getRollbackOnly("onMessage()");

        testSLLaHomeAccess(ejbJndiName2);
        testSLLaObjectAccess(ejbJndiName2);

        // testBMPLocal();
        // testBMPRemote();
        // testCMPRemoteAccess();

        try {
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
     * Execute getCallerPrincipal
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
     * Execute isCallerInRole
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
        }
    }

    /**
     * Execute getEJBHome
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
     * Execute getEJBLocalHome
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
     * Access SF Local interface from MDB
     *
     * @param jndiName JNDI Name of the SF Local home
     */
    public void testJNDIAccess(String jndiName) {
        try {
            SFLocalHome fhome1 = (SFLocalHome) new InitialContext().lookup(jndiName);
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
     * Access SL Local interface from MDB
     *
     * @param jndiName JNDI Name of the SL Local home
     * @param results test point results
     */
    public void testSLLaHomeAccess(String jndiName) {
        try {
            SLLaHome fhome1 = (SLLaHome) new InitialContext().lookup(jndiName);
            if (fhome1 == null) {
                results = results + " FAIL: The SLLaHome JNDI resource is null. ";
            } else {
                results = results + " The SLLaHome JNDI resource is found and not null. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing SLLaHome JNDI: " + e.toString() + ". ";
        }
    }

    /**
     * Access SL Local interface from MDB and access the methods of SL
     */
    public void testSLLaObjectAccess(String jndiName) {
        SLLaHome fhome1 = null;
        SLLa fejb1 = null;

        try {
            fhome1 = (SLLaHome) new InitialContext().lookup(jndiName);
            fejb1 = fhome1.create();

            String testStr = "Test string.";
            String buf = fejb1.method1(testStr);
            if (buf.equalsIgnoreCase(testStr)) {
                svLogger.info("testSLLaObjectAccess passed.");
            } else {
                results = results + " FAIL: fejb1.method1 returns a wrong value: " + buf + ". ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception caught in testSLLaObjectAccess: " + e.toString() + ". ";
        }
    }

    /**
     * Execute getUserTransaction
     *
     * @param invokeLoc location MDB is invoked from
     * @throws Exception
     */
    public void ctx_getUserTransaction(String invokeLoc) {
        try {
            UserTransaction transaction = myMessageDrivenCtx.getUserTransaction();
            transaction.getStatus();
            results = results + " FAIL: A UserTransaction interface should not be successfully accessed from " + invokeLoc + " in CMT MDB. ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when invoking getUserTransaction in a CMT MDB. ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when accessing a UserTransaction interface from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
        }
    }

    /**
     * Execute getRollbackOnly
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getRollbackOnly(String invokeLoc) {
        try {
            myMessageDrivenCtx.getRollbackOnly();
            svLogger.info("CMTD MDB can access getRollbackOnly() when " + invokeLoc);
        } catch (IllegalStateException ise) {
            results = results + " FAIL: IllegalStateException was incorrectly thrown when invoking getRollbackOnly from " + invokeLoc + " in a CMT MDB. ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking getRollbackOnly from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
        }
    }

    /**
     * Execute getRollbackOnly and expect failures
     *
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getRollbackOnlyFailed(String invokeLoc) {
        try {
            myMessageDrivenCtx.getRollbackOnly();
            results = results + " FAIL: IllegalStateException should be thrown when getRollbackOnly is invoked from " + invokeLoc + ". ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " in MDB invokes getRollbackOnly(). ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking getRollbackOnly from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
        }
    }
}