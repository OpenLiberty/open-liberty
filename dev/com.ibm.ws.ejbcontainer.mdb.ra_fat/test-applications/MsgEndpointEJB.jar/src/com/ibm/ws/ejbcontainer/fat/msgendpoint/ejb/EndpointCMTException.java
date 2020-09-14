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
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

public class EndpointCMTException implements MessageListener {
    public static boolean mdbInvokedTheFirstTime = true;
    public static CountDownLatch svMsgLatch;

    @Resource
    MessageDrivenContext myMessageDrivenCtx;

    @Resource
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        // Nothing to do
    }

    public static void initMsgLatch() {
        svMsgLatch = new CountDownLatch(1);
    }

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() {
        System.out.println("--EndpointCMTException: ejbCreate");
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    public void ejbRemove() {
    }

    /**
     * <p>Passes a message to the listener.
     *
     * @param message a message
     */
    @Override
    public void onMessage(Message message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("EndpointCMTException is in a global transaction");
        } else {
            System.out.println("EndpointCMTException is in a local transaction");
        }
        System.out.println("--onMessage: EndpointCMTException got the message :" + message);
    }

    /**
     * <p>Passes a String message to the listener.
     *
     * @param message a String message
     */
    @Override
    public void onStringMessage(String message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("EndpointCMTException is in a global transaction");
        } else {
            System.out.println("EndpointCMTException is in a local transaction");
        }
        System.out.println("--onStringMessage: EndpointCMTException got the message :" + message);

        if (mdbInvokedTheFirstTime) {
            mdbInvokedTheFirstTime = false;
            System.out.println("The first time to enter onStringMessage. Throw RuntimeException.");
            throw new RuntimeException();
        } else {
            System.out.println("The second time to enter onStringMessage. No exception thrown");
            if (svMsgLatch != null)
                svMsgLatch.countDown();
        }

    }

    /**
     * <p>Passes an Integermessage to the listener.
     *
     * @param message an Integer message
     */
    @Override
    public void onIntegerMessage(Integer message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("EndpointCMTException is in a global transaction");
        } else {
            System.out.println("EndpointCMTException is in a local transaction");
        }
        System.out.println("--onIntegerMessage: EndpointCMTException got the message :" + message);
    }

    /**
     * <p>onCreateDBEntryNikki from tra.MessageListner interface
     * This method will be used by J2C FVT to create a db entry
     * in cmtest table in jtest1 database.
     *
     * @param message a String message
     */
    @Override
    public void onCreateDBEntryNikki(String message) {
    }

    /**
     * <p>onCreateDBEntryZiyad from tra.MessageListner interface
     * This method will be used by J2C FVT to create a db entry
     * in cmtest table in jtest1 database.
     *
     * @param message a String message
     */
    @Override
    public void onCreateDBEntryZiyad(String message) {
    }

    /**
     * <p>onWait from tra.MessageListner interface
     * This method will be used by J2C FVT to wait for x seconds specified
     * by the message when message is received. This is used
     * when we want to do prepare call before the submitted Work is completed.
     *
     * @param msg a String message, represents the wait time in seconds.
     */
    @Override
    public void onWait(String msg) {
    }

    /**
     * <p>onGetTimestamp from tra.MessageListner interface
     * This method will be used by J2C FVT to get a system time when message is
     * received. This is used for validating when each message is
     * received to make sure the message is sent periodically according to the
     * period specified.
     *
     * Details to be determined later.
     *
     * @param msg a String message
     */
    @Override
    public void onGetTimestamp(String msg) {
    }

    // d174256
    /**
     * onThrowEJBException from tra.MessageListner interface
     * This method will throw EJB exceptions. This is used for validating if there are
     * any problem in stopping the server after MDB throws this exception.
     */
    @Override
    public void onThrowEJBException(String msg) {
    }
}
