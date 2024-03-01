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

package org.apache.cxf.wsdl.interceptors;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.common.logging.LogUtils;


public class BareOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(BareOutInterceptor.class);  // Liberty Change

    public BareOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(Message message) {
        Exchange exchange = message.getExchange();
        BindingOperationInfo operation = exchange.getBindingOperationInfo();

        if (operation == null) {
	    LOG.finest("BareOutInterceptor: Operation is NULL, returning..."); // Liberty Change start
            return;
        }

        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.isEmpty()) {
	    LOG.finest("BareOutInterceptor: MessageContentsList is empty, returning...");
            return;
        }

        final List<MessagePartInfo> parts;
        final BindingMessageInfo bmsg;
        boolean client = isRequestor(message);

        if (!client) {
            if (operation.getOutput() != null) {
		LOG.fine("BareOutInterceptor: Getoutput for operation: " + operation.getName());
                bmsg = operation.getOutput();
                parts = bmsg.getMessageParts();
            } else {
                // partial response to oneway
		LOG.finest("BareOutInterceptor: Operation output is NULL, returning");
                return;
            }
        } else {
	    LOG.fine("BareOutInterceptor: Get input message parts...");
            bmsg = operation.getInput();
            parts = bmsg.getMessageParts();
        }

	if (LOG.isLoggable(Level.FINEST)) {
	   for (MessagePartInfo mp1 : parts) {
	      LOG.finest("BareOutInterceptor: Msg Part: " + mp1.toString());  // Liberty Change End
	   }
	}

        writeParts(message, exchange, operation, objs, parts);
    }

}
