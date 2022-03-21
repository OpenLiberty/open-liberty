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

package org.apache.cxf.ws.policy.mtom;

import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;

public class MTOMPolicyInterceptor extends AbstractPhaseInterceptor<Message> {
    public MTOMPolicyInterceptor() {
        super(Phase.POST_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(MetadataConstants.MTOM_ASSERTION_QNAME);
            for (AssertionInfo ai : ais) {
                if (MessageUtils.isRequestor(message)) {
                    //just turn on MTOM
                    message.put(Message.MTOM_ENABLED, Boolean.TRUE);
                    ai.setAsserted(true);
                } else {
                    // set mtom enabled and assert the policy if we find an mtom request
                    String contentType = (String)message.getExchange().getInMessage()
                        .get(Message.CONTENT_TYPE);
                    if (contentType != null && contentType.contains("type=\"application/xop+xml\"")) {
                        ai.setAsserted(true);
                        message.put(Message.MTOM_ENABLED, Boolean.TRUE);
                    }
                }
            }
        }
    }
}
