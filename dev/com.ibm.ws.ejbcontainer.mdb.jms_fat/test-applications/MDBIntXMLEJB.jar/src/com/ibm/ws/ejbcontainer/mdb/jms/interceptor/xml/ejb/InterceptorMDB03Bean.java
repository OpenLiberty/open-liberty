/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.ejb;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.EJBContext;
import javax.interceptor.InvocationContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a Message Driven Bean (MCM03) that on receipt of a message prints the messageid and the contents of the message to the standard out of the Application Server
 */
// the following annotations were defined in xml
// @MessageDriven
// @Interceptors( { Interceptor02.class, Interceptor01.class })
public class InterceptorMDB03Bean implements MessageListener {
    private static final String LOGGER_CLASS_NAME = InterceptorMDB03Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    public static CountDownLatch svDestroyLatch;

    // d459309
    private QueueConnectionFactory replyQueueFactoryName;
    private Queue replyQueueName;

    final static String BeanName = "InterceptorMDB03Bean";

    private static String newline = System.getProperty("line.separator");

    private EJBContext ctx; // injected in xml

    private SimpleSLLocal injectedRef;

    // Inject a simple stateless bean into the ENC for this setter method in xml
    public void setSimpleSL(SimpleSLLocal injectedRef) {
        this.injectedRef = injectedRef;
    }

    /**
     * The onMessage method extracts the text from the message and the messageid and passes that data into the handleMessage methods.
     *
     * @param msg
     *            javax.jms.Message This should be a TextMessage.
     */
    // @ExcludeDefaultInterceptors, injected in xml
    @Override
    public void onMessage(Message msg) {
        String rcvMsg = null;
        String result = null;

        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB03Bean.onMessage", this);
        svLogger.info("InterceptorMDB03Bean.onMessage: this=" + this);

        try {
            rcvMsg = ((TextMessage) msg).getText();
            svLogger.info("onMessage() text received: " + rcvMsg);

            if (rcvMsg.equals("AroundInvoke") || rcvMsg.equals("PostConstruct") || rcvMsg.equals("PreDestroy")) {
                List<String> callList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                String callListStr = Arrays.toString(new String[0]);
                if (callList != null) {
                    callListStr = Arrays.toString(callList.toArray(new String[callList.size()]));
                }

                FATMDBHelper.putQueueMessage(callListStr, replyQueueFactoryName, replyQueueName);
            } else if (rcvMsg.equals("CallInjEJB")) {
                String expected = "success";

                if (expected.equals(injectedRef.getString())) {
                    // Test class will check for this value
                    result = "Successfully invoked the getString() on the injected bean and it returned: " + injectedRef.getString() + " from the method call." + newline;
                } else {
                    result = "Value from bean was not expected value. injectedRef.getString() = " + injectedRef.getString() + newline;
                }

                if (ctx == null) {
                    result += "Session context not injected --> ctx == null." + newline;
                }

                // Lookup the stateful bean using the default ENC JNDI entry that should
                // have been added by default via the method level injection
                // use <ejb-ref-name>SimpleSLLocal</ejb-ref-name> in ejb-jar.xml
                SimpleSLLocal obj = (SimpleSLLocal) ctx.lookup("SimpleSLLocal");

                result += "Just completed lookup of com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.ejb.InterceptorMDB03Bean/simpleSL." + newline;

                // Call a method on the bean to ensure that the ref is valid
                if (expected.equals(obj.getString())) {
                    // Test class will check for this value
                    result += "Successfully looked up the injected EJB via the default ENC created by injecting the EJB at the " + "method level and received expected message: "
                              + obj.getString() + " from the method call.";
                } else {
                    result += "Value from bean was not expected value. obj.getString() = " + obj.getString();
                }

                FATMDBHelper.putQueueMessage(result, replyQueueFactoryName, replyQueueName);
            } else if (rcvMsg.equals("ClearAll")) {
                CheckInvocation.getInstance().clearAllCallInfoList();

                FATMDBHelper.putQueueMessage("finished clear", replyQueueFactoryName, replyQueueName);
            }
        } catch (Throwable t) {
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    // @AroundInvoke, defined in xml
    @SuppressWarnings("unused")
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB03Bean.aroundInvoke", this);
        svLogger.info("InterceptorMDB03Bean.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    // @PostConstruct, defined in xml
    public void postConstructCallback() {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "InterceptorMDB03Bean.postConstruct", this);
        svLogger.info("InterceptorMDB03Bean.postConstruct: this=" + this);
        svDestroyLatch = new CountDownLatch(1);
    }

    // @PreDestroy, defined in xml
    public void preDestroyCallback() {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "InterceptorMDB03Bean.preDestroy", this);
        svLogger.info("InterceptorMDB03Bean.preDestroy: this=" + this);
        svDestroyLatch.countDown();
    }
}