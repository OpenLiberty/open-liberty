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

package org.apache.cxf.jaxws.interceptors;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;


public class HolderOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(HolderOutInterceptor.class);

    public HolderOutInterceptor() {
        super(Phase.PRE_LOGICAL);
        addBefore(WrapperClassOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        MessageContentsList outObjects = MessageContentsList.getContentsList(message);
        Exchange exchange = message.getExchange();
        OperationInfo op = exchange.getBindingOperationInfo() == null
            ? null
                : exchange.getBindingOperationInfo().getOperationInfo();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("op: " + op);
            if (null != op) {
                LOG.fine("op.hasOutput(): " + op.hasOutput());
                if (op.hasOutput()) {
                    LOG.fine("op.getOutput().size(): " + op.getOutput().size());
                }
            }
        }

        if (op == null || !op.hasOutput() || op.getOutput().size() == 0) {
            LOG.fine("Returning.");
            return;
        }

        if (!Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE))) {
            List<MessagePartInfo> parts = op.getOutput().getMessageParts();
            MessageContentsList inObjects = MessageContentsList.getContentsList(exchange.getInMessage());
            if (inObjects != null) {
                if (inObjects != outObjects) {
                    for (int x = 0; x < inObjects.size(); x++) {
                        Object o = inObjects.get(x);
                        if (o instanceof Holder) {
                            outObjects.set(x + 1, o);
                        }
                    }
                } else {
                    LOG.severe("CANNOT_SET_HOLDER_OBJECTS");
                    throw new Fault(new org.apache.cxf.common.i18n.Message("CANNOT_SET_HOLDER_OBJECTS", LOG));
                }
            }
            for (MessagePartInfo part : parts) {
                if (part.getIndex() > 0 && part.getTypeClass() != null) {
                    Holder<?> holder = (Holder<?>)outObjects.get(part);
                    outObjects.put(part, holder.value);
                }
            }
        } else {
            List<Object> holders = new ArrayList<>(outObjects);
            for (int x = 0; x < outObjects.size(); x++) {
                Object o = outObjects.get(x);
                if (o instanceof Holder) {
                    outObjects.set(x, ((Holder<?>)o).value);
                } else {
                    holders.set(x, null);
                }
            }
            message.put(HolderInInterceptor.CLIENT_HOLDERS, holders);
        }

    }
}
