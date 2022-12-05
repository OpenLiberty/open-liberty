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

package org.apache.cxf.interceptor;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;

/**
 * Takes the Conduit from the exchange and sends the message through it.
 */
public class MessageSenderInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(MessageSenderInterceptor.class);
    private MessageSenderEndingInterceptor ending = new MessageSenderEndingInterceptor();


    public MessageSenderInterceptor() {
        super(Phase.PREPARE_SEND);
    }

    public void handleMessage(Message message) {
        try {
            getConduit(message).prepare(message);
        } catch (IOException ex) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_SEND", BUNDLE), ex);
        }

        // Add a final interceptor to close the conduit
        message.getInterceptorChain().add(ending);
    }

    public static class MessageSenderEndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public MessageSenderEndingInterceptor() {
            super(Phase.PREPARE_SEND_ENDING);
        }

        public void handleMessage(Message message) {
            try {
                getConduit(message).close(message);
            } catch (SocketTimeoutException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_RECEIVE", BUNDLE), e);
            } catch (IOException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_SEND", BUNDLE), e);
            }
        }
    }

    public static Conduit getConduit(Message message) throws IOException {
        Exchange exchange = message.getExchange();
        Conduit conduit = exchange.getConduit(message);
        if (conduit == null
            && (exchange.getOutMessage() != null
                || exchange.getOutFaultMessage() != null)) {
            conduit = OutgoingChainInterceptor.getBackChannelConduit(message);
        }
        return conduit;
    }

}
