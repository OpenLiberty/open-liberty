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
import org.apache.cxf.common.logging.LogUtils;
import java.util.logging.Logger;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;

import com.ibm.websphere.ras.annotation.Sensitive; // Liberty Change


// Liberty Change; This class has no Liberty specific changes other than the Sensitive annotation 
// It is required as an overlay because of Liberty specific changes to MessageImpl.put(). Any call
// to SoapMessage.put() will cause a NoSuchMethodException in the calling class if the class is not recompiled.
// If a solution to this compilation issue can be found, this class should be removed as an overlay. 

public class MTOMPolicyInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(MTOMPolicyInterceptor.class);  // Liberty Change

    public MTOMPolicyInterceptor() {
        super(Phase.POST_LOGICAL);
    }

    public void handleMessage(@Sensitive Message message) throws Fault { // Liberty Change
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(MetadataConstants.MTOM_ASSERTION_QNAME);
            for (AssertionInfo ai : ais) {
                if (MessageUtils.isRequestor(message)) {
                    //just turn on MTOM
		    LOG.fine("Enable MTOM on client side");  // Liberty Change start
                    message.put(Message.MTOM_ENABLED, Boolean.TRUE);
                    ai.setAsserted(true);
                } else {
                    // set mtom enabled and assert the policy if we find an mtom request
                    String contentType = (String)message.getExchange().getInMessage()
                        .get(Message.CONTENT_TYPE);
		    LOG.fine("ContentType is " + contentType);
                    if (contentType != null && contentType.contains("type=\"application/xop+xml\"")) {
                        ai.setAsserted(true);
		        LOG.fine("Enable MTOM on provider side");  // Liberty Change end
                        message.put(Message.MTOM_ENABLED, Boolean.TRUE);
                    }
                }
            }
        }
    }
}
