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

import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

public class ConcurCMTException implements MessageListener {
    @Resource
    MessageDrivenContext myMessageDrivenCtx;

    public static Object syncObject = null;
    public static ConcurrencyInfo concurrentInfo = null;
    public static boolean mdbInvokedTheFirstTime = true;

    @Resource
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        // Nothing to do
    }

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() {
        System.out.println("--ConcurCMTException: ejbCreate");
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
            System.out.println("ConcurCMTException is in a global transaction");
        } else {
            System.out.println("ConcurCMTException is in a local transaction");
        }
        System.out.println("--onMessage: ConcurCMTException got the message :" + message);
    }

    /**
     * <p>Passes a String message to the listener.
     *
     * @param message a String message
     */
    @Override
    public void onStringMessage(String message) {
        if (AdapterUtil.inGlobalTransaction()) {
            System.out.println("ConcurCMTException is in a global transaction");
        } else {
            System.out.println("ConcurCMTException is in a local transaction");
        }
        System.out.println("--ConcurCMTException (" + message + "): entering onStringMessage");

        if (mdbInvokedTheFirstTime) {
            concurrentInfo.decreaseConcurrentMsgNumber();
            System.out.println("--ConcurCMTException (" + message + "): after decreaseConcurrentMsgNumber");

            synchronized (syncObject) {
                System.out.println("--ConcurCMTException (" + message + "): in the synchronized block");

                while (concurrentInfo.getConcurrentMsgNumber() > 0) {
                    System.out.println("--ConcurCMTException (" + message + "): in the while block");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        System.out.println("InterruptedException caught in onStringMessage!");
                        throw new RuntimeException();
                    }
                }
                syncObject.notifyAll();
                System.out.println("--ConcurCMTException (" + message + "): after notifyAll() in the synchronized block");
            }

            System.out.println("The first time to enter onStringMessage. Runtime exception thrown");
            mdbInvokedTheFirstTime = false;
            System.out.println("The first time to enter onStringMessage. Throw RuntimeException.");
            throw new RuntimeException();
        } else {
            System.out.println("The second time to enter onStringMessage. No exception thrown");
        }

        System.out.println("--ConcurCMTException (" + message + "): exiting onStringMessage.");
    }

    /**
     * <p>Passes an Integermessage to the listener.
     *
     * @param message an Integer message
     */
    @Override
    public void onIntegerMessage(Integer message) {
        System.out.println("--onStringMessage: ConcurCMTException got the message :" + message);
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
