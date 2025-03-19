/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.management.interceptor;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class ResponseTimeMessageOutInterceptor extends AbstractMessageResponseTimeInterceptor {
    private EndingInterceptor ending = new EndingInterceptor();

    Logger log = Logger.getLogger(ResponseTimeMessageOutInterceptor.class.getName());
    
    public ResponseTimeMessageOutInterceptor() {
        super(Phase.PREPARE_SEND_ENDING);
        addBefore(MessageSenderInterceptor.MessageSenderEndingInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        // Liberty change begin
        boolean forceDisabled = getForceDisabled(ex);
        if (!forceDisabled && isServiceCounterEnabled(ex)) {
         // Liberty change end
            if (ex.get(Exception.class) != null) {
                endHandlingMessage(ex);
                return;
            }
            if (Boolean.TRUE.equals(message.get(Message.PARTIAL_RESPONSE_MESSAGE))) {
                return;
            }
            if (isClient(message)) {
                if (ex.isOneWay()) {
                    message.getInterceptorChain().add(ending);
                }
                beginHandlingMessage(ex);
            } else { // the message is handled by server
                endHandlingMessage(ex);
            }
        }
    }
    
    // Liberty change begin
    /* 
     * Disable counter either org.apache.cxf.management.counter.enabled property is set 
     * or interface name contains special character causing IllegalArgumentException 
     * according to JMX specification. Code logic below prevents FFDC creation by disabling 
     * counter for name spaces with special characters
     */
    private boolean getForceDisabled(Exchange ex)  {
        boolean forceDisabled = Boolean.FALSE.equals(ex.get("org.apache.cxf.management.counter.enabled"));
 
        Object object = ex.get("javax.xml.ws.wsdl.interface");
        if(object != null && object instanceof QName)     {
            String namespaceURI = ((QName) object).getNamespaceURI();
            if (!namespaceURI.matches("[a-zA-Z0-9./:?_]*$")) {
                forceDisabled = true;
            }
        }
        return forceDisabled;
    }
    // Liberty change end

    @Override
    public void handleFault(Message message) {
        Exchange ex = message.getExchange();
        if (ex.get("org.apache.cxf.management.counter.enabled") != null) {
            if (ex.isOneWay()) {
                // do nothing, done by the ResponseTimeInvokerInterceptor
            } else {
                FaultMode faultMode = message.get(FaultMode.class);
                if (faultMode == null) {
                    // client side exceptions don't have FaultMode set un the message properties (as of 2.1.4)
                    faultMode = FaultMode.RUNTIME_FAULT;
                }
                ex.put(FaultMode.class, faultMode);
                endHandlingMessage(ex);
            }
        }
    }

    EndingInterceptor getEndingInterceptor() {
        return ending;
    }

    public class EndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public EndingInterceptor() {
            super(Phase.PREPARE_SEND_ENDING);
        }

        public void handleMessage(Message message) throws Fault {
            Exchange ex = message.getExchange();
            endHandlingMessage(ex);
        }

        public void handleFault(Message message) throws Fault {
            Exchange ex = message.getExchange();
            endHandlingMessage(ex);
        }
    }
}
