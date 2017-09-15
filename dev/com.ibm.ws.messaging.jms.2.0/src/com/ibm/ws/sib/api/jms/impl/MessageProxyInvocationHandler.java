/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jms.JMSException;
import javax.jms.Message;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Invocation handler for message object. This is used only for AsyncSend.. so this would
 * be created only in Thin client environment
 */
class MessageProxyInvocationHandler implements InvocationHandler {
    private static TraceComponent tc = SibTr.register(MessageProxyInvocationHandler.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    private final Message actualMessageObject;

    MessageProxyInvocationHandler(Message msgObject) {
        actualMessageObject = msgObject;

    }

    Message getMessage() {
        return actualMessageObject;

    }

    /**
     * While Async send operation is in progress, do not allow application to access message object
     */

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "invoke", method.toGenericString());

        if (actualMessageObject instanceof JmsMessageImpl) {

            if (((JmsMessageImpl) actualMessageObject).isAsynSendInProgress()) {
                //Async send is in progress... application is trying to access message object
                //throw JMSException

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Application is trying to access message object.. when asysn send is in progress.. throwing JMSException");

                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "INVALID_MESSAGE_ACCESS_CWSIA0516E",
                                                                null, // object[] argument
                                                                tc
                                );

            }
        }

        Object retObject = null;

        try {

            retObject = method.invoke(actualMessageObject, args);

        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "invoke", retObject);

        //call the method on actual message object.
        return retObject;

    }
}