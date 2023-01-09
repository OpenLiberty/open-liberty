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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;
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
// add annotation for activationConfig props
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "MDBReqQueue") })
@ExcludeDefaultInterceptors
@Interceptors({ Interceptor01.class, Interceptor02.class })
@EJB(name = "ejb/SLEnvInjectTest_local_biz", beanName = "SLEnvInjectTest", beanInterface = SimpleSLLocal.class)
public class InterceptorMDB01Bean implements MessageListener {
    private static final String CLASS_NAME = InterceptorMDB01Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static CountDownLatch svDestroyLatch;

    // d459309, add authenticationType=APPLICATION
    @Resource(name = "jms/TestQCF", authenticationType = APPLICATION, shareable = true)
    private QueueConnectionFactory replyQueueFactory;

    @Resource(name = "jms/TestResultQueue")
    private Queue replyQueue;

    final static String BeanName = "InterceptorMDB01Bean";

    private static String newline = System.getProperty("line.separator");

    @Resource
    private EJBContext ctx;

    /**
     * The onMessage method extracts the text from the message and the messageid and passes that data into the handleMessage methods.
     *
     * @param msg
     *            javax.jms.Message This should be a TextMessage.
     */
    @Override
    @Interceptors({ Interceptor03.class, Interceptor04.class })
    public void onMessage(Message msg) {
        String rcvMsg = null;
        String result = null;

        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB01Bean.onMessage", this);
        svLogger.info("InterceptorMDB01Bean.onMessage: this=" + this);

        try {
            rcvMsg = ((TextMessage) msg).getText();
            svLogger.info("onMessage() text received: " + rcvMsg);

            if (rcvMsg.equals("AroundInvoke") || rcvMsg.equals("PostConstruct") || rcvMsg.equals("PreDestroy")) {
                List<String> callList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                String callListStr = Arrays.toString(new String[0]);
                if (callList != null) {
                    callListStr = Arrays.toString(callList.toArray(new String[callList.size()]));
                }
                FATMDBHelper.putQueueMessage(callListStr, replyQueueFactory, replyQueue);
            } else if (rcvMsg.equals("CallInjEJB")) {
                if (ctx == null) {
                    result = "Failed: Session context not injected --> ctx == null.";
                }

                // Lookup the stateless bean using an injected session context, using the ENC
                // JNDI entry added by class level injection
                Object obj = ctx.lookup("ejb/SLEnvInjectTest_local_biz");
                result = "Just completed lookup of ejb/SLEnvInjectTest_local_biz." + newline;
                SimpleSLLocal injectedRef = (SimpleSLLocal) obj;
                String expected = "success";
                // Call a method on the bean to ensure that the ref is valid
                if (expected.equals(injectedRef.getString())) {
                    // Test class will check for this value
                    result += "Successfully looked up the injected EJB and received expected message: " + injectedRef.getString() + " from the method call.";
                } else {
                    result += "Value from bean was not expected value. injectedRef.getString() = " + injectedRef.getString();
                }

                FATMDBHelper.putQueueMessage(result, replyQueueFactory, replyQueue);
            } else if (rcvMsg.equals("ClearAll")) {
                CheckInvocation.getInstance().clearAllCallInfoList();
                FATMDBHelper.putQueueMessage("finished clear", replyQueueFactory, replyQueue);
            }
        } catch (Throwable t) {
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB01Bean.aroundInvoke", this);
        svLogger.info("InterceptorMDB01Bean.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    public void postConstruct() {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "InterceptorMDB01Bean.postConstruct", this);
        svLogger.info("InterceptorMDB01Bean.postConstruct: this=" + this);
        svDestroyLatch = new CountDownLatch(1);
    }

    @PreDestroy
    public void preDestroy() {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "InterceptorMDB01Bean.preDestroy", this);
        svLogger.info("InterceptorMDB01Bean.preDestroy: this=" + this);
        svDestroyLatch.countDown();
    }
}