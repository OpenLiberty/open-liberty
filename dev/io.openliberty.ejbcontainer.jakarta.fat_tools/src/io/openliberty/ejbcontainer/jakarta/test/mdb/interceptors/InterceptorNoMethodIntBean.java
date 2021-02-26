/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.mdb.interceptors;

import static jakarta.annotation.Resource.AuthenticationType.APPLICATION;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import io.openliberty.ejbcontainer.jakarta.test.tools.FATMDBHelper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.MessageDriven;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Record;

@MessageDriven
@Interceptors({ Interceptor01.class, Interceptor02.class })
public class InterceptorNoMethodIntBean implements InterceptorNoMethodInterface {

    private final static Logger svLogger = Logger.getLogger("InterceptorNoMethodIntBean");

    @Resource(name = "jms/TestQCF", authenticationType = APPLICATION, shareable = true)
    private QueueConnectionFactory replyQueueFactory;

    @Resource(name = "jms/TestResultQueue")
    private Queue resultQueue;

    public Record ADD(Record record) throws ResourceException {
        svLogger.info("InterceptorNoMethodIntBean.ADD record = " + record);
        return record;
    }

    @Interceptors({ Interceptor03.class, Interceptor04.class })
    public void INTERCEPTOR(String rcvMsg) throws ResourceException {
        svLogger.info("INTERCEPTOR() received: " + rcvMsg);

        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorNoMethodIntBean.INTERCEPTOR", this);
        svLogger.info("InterceptorNoMethodIntBean.INTERCEPTOR: this=" + this);

        try {
            if (rcvMsg.equals("AroundInvoke") || rcvMsg.equals("PostConstruct") || rcvMsg.startsWith("PreDestroy")) {
                List<String> callList = CheckInvocation.getInstance().clearCallInfoList(rcvMsg);
                String callListStr = Arrays.toString(new String[0]);
                if (callList != null) {
                    callListStr = Arrays.toString(callList.toArray(new String[callList.size()]));
                }
                FATMDBHelper.putQueueMessage(callListStr, replyQueueFactory, resultQueue);
            } else if (rcvMsg.equals("Print")) {
                FATMDBHelper.putQueueMessage("InterceptorNoMethodIntBean.INTERCEPTOR " + rcvMsg, replyQueueFactory, resultQueue);
                svLogger.info("InterceptorNoMethodIntBean.INTERCEPTOR " + rcvMsg);
            } else if (rcvMsg.equals("SetupPreDestroy")) {
                CheckInvocation.getInstance().setupPreDestroy();
                FATMDBHelper.putQueueMessage("finished setup", replyQueueFactory, resultQueue);
            } else if (rcvMsg.equals("ClearAll")) {
                CheckInvocation.getInstance().clearAllCallInfoList();
                FATMDBHelper.putQueueMessage("finished clear", replyQueueFactory, resultQueue);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Record privateOnMessage(Record record) throws ResourceException {
        svLogger.info("Private method should not be reachable!");
        return record;
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "InterceptorNoMethodIntBean.aroundInvoke", this);
        svLogger.info("InterceptorMDB01Bean.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    public void postConstruct() {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "InterceptorNoMethodIntBean.postConstruct", this);
        svLogger.info("InterceptorMDB01Bean.postConstruct: this=" + this);
    }

    @PreDestroy
    public void preDestroy() {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "InterceptorNoMethodIntBean.preDestroy", this);
        svLogger.info("InterceptorMDB01Bean.preDestroy: this=" + this);
    }
}
