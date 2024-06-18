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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class HolderInInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String CLIENT_HOLDERS = "client.holders";

    private static final Logger LOG = LogUtils.getLogger(HolderInInterceptor.class); // Liberty Change issue #26529
    public HolderInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(Message message) throws Fault {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        MessageContentsList inObjects = MessageContentsList.getContentsList(message);

        if(isFinestEnabled)  {
            LOG.finest("MessageContentsList that is obtained from message: " + inObjects);   // Liberty Change issue #26529
        }        
        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.getBindingOperationInfo();
        if (bop == null) {
            if(isFinestEnabled)  {
                LOG.finest("BindingOperationInfo is null. handleMessage won't be executing.");   // Liberty Change issue #26529
            } 
            return;
        }
        OperationInfo op = bop.getOperationInfo();
        if (op == null || !op.hasOutput() || op.getOutput().size() == 0) {
            if(isFinestEnabled)  {
                LOG.finest("OperationInfo is null or empty. handleMessage won't be executing.");   // Liberty Change issue #26529
            } 
            return;
        }

        List<MessagePartInfo> parts = op.getOutput().getMessageParts();

        boolean client = isRequestor(message);
        if(isFinestEnabled)  {
            LOG.finest("Is requestor: " + client);   // Liberty Change issue #26529
        } 
        if (client) {
            List<Holder<?>> outHolders = CastUtils.cast((List<?>)message.getExchange()
                .getOutMessage().get(CLIENT_HOLDERS));
            for (MessagePartInfo part : parts) {
                if (part.getIndex() != 0 && part.getTypeClass() != null) {
                    @SuppressWarnings("unchecked")
                    Holder<Object> holder = (Holder<Object>)outHolders.get(part.getIndex() - 1);
                    if (holder != null) {
                        holder.value = inObjects.get(part);
                        inObjects.put(part, holder);
                        if(isFinestEnabled)  {
                            LOG.finest("Holder added to message content list for client side: " + holder);   // Liberty Change issue #26529
                        } 
                    }
                }
            }
        } else {
            for (MessagePartInfo part : parts) {
                int idx = part.getIndex() - 1;
                if (idx >= 0 && part.getTypeClass() != null) {
                    if (inObjects == null) {
                        //if soap:body is empty, the contents may not exist
                        //so we need to create a contents list to store
                        //the holders for the outgoing parts (CXF-4031)
                        inObjects = new MessageContentsList();
                        message.setContent(List.class, inObjects);
                        if(isFinestEnabled)  {
                            LOG.finest("Null message contents list is replaced with an empty one for server side.");   // Liberty Change issue #26529
                        }
                    }
                    if (idx >= inObjects.size()) {
                        inObjects.set(idx, new Holder<Object>());
                        if(isFinestEnabled)  {
                            LOG.finest("Message contents list is enlarged to match message parts size for server side with new empty Holder<Object>().");   // Liberty Change issue #26529
                        }
                    } else {
                        Object o = inObjects.get(idx);
                        inObjects.set(idx, new Holder<Object>(o));
                        if(isFinestEnabled)  {
                            LOG.finest("The object in message contents list is wrapped in a holder:" + inObjects.get(idx));   // Liberty Change issue #26529
                        }
                    }
                }
            }
        }
    }
}
