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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a Message Driven Bean (MCM03) that on receipt of a message prints the messageid and the contents of the message to the standard out of the Application Server
 */
@MessageDriven
@Interceptors({ Interceptor01.class, Interceptor02.class })
public class InterceptorMDB02Bean implements MessageListener {
    private static final String LOGGER_CLASS_NAME = InterceptorMDB02Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    public static CountDownLatch svDestroyLatch;

    private final String replyQueueFactoryName = "java:comp/env/jms/TestQCF";

    private final String replyQueueName = "java:comp/env/jms/TestResultQueue";

    final static String BeanName = "InterceptorMDB02Bean";

    final static String MethodName = "onMessage";
    private static String newline = System.getProperty("line.separator");

    @Resource
    private EJBContext ctx;

    // Inject a simple stateless bean into the ENC for this field
    @EJB
    private SimpleSLLocal injectedRef;

    /**
     * The onMessage method extracts the text from the message and the messageid and passes that data into the handleMessage methods.
     *
     * @param msg
     *            javax.jms.Message This should be a TextMessage.
     */
    @Override
    @ExcludeDefaultInterceptors
    @ExcludeClassInterceptors
    @Interceptors({ Interceptor04.class, Interceptor03.class })
    public void onMessage(Message msg) {
        String rcvMsg = null;
        String result = null;

        svLogger.info("InterceptorMDB02Bean: this=" + this);
        svLogger.info("InterceptorMDB02Bean: method=" + MethodName);
        svLogger.info("InterceptorMDB02Bean: parameter=" + Arrays.toString(new Object[] { msg }));

        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB02Bean.onMessage", this);
        svLogger.info("InterceptorMDB02Bean.onMessage: this=" + this);

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
            } else if (rcvMsg.equals("Target")) {
                List<String> targetList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                boolean eqChk = targetList.get(0).equals(targetList.get(1));
                eqChk &= targetList.get(0).equals(this.toString());

                FATMDBHelper.putQueueMessage(Boolean.toString(eqChk), replyQueueFactoryName, replyQueueName);
            } else if (rcvMsg.equals("Method")) {
                List<String> methodList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                boolean eqChk = methodList.get(0).equals(methodList.get(1));
                eqChk &= methodList.get(0).indexOf(MethodName) >= 0;

                FATMDBHelper.putQueueMessage(Boolean.toString(eqChk), replyQueueFactoryName, replyQueueName);
            } else if (rcvMsg.equals("Parameters")) {
                List<String> parametersList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                boolean eqChk = parametersList.get(0).equals(parametersList.get(1));
                eqChk &= parametersList.get(0).equals(Arrays.toString(new Object[] { msg }));

                FATMDBHelper.putQueueMessage(Boolean.toString(eqChk), replyQueueFactoryName, replyQueueName);
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
                // have been added by default via the field level injection
                SimpleSLLocal obj = (SimpleSLLocal) ctx.lookup("com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb.InterceptorMDB02Bean/injectedRef");

                result += "Just completed lookup of com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb.InterceptorMDB02Bean/injectedRef." + newline;

                // Call a method on the bean to ensure that the ref is valid
                if (expected.equals(obj.getString())) {
                    // Test class will check for this value
                    result += "Successfully looked up the injected EJB via the default ENC created by injecting the EJB at the " + "field level and received expected message: "
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

    @AroundInvoke
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorMDB02Bean.aroundInvoke", this);
        svLogger.info("InterceptorMDB02Bean.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    public void postConstruct() {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "InterceptorMDB02Bean.postConstruct", this);
        svLogger.info("InterceptorMDB02Bean.postConstruct: this=" + this);
        svDestroyLatch = new CountDownLatch(1);
    }

    @PreDestroy
    public void preDestroy() {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "InterceptorMDB02Bean.preDestroy", this);
        svLogger.info("InterceptorMDB02Bean.preDestroy: this=" + this);
        svDestroyLatch.countDown();
    }
}