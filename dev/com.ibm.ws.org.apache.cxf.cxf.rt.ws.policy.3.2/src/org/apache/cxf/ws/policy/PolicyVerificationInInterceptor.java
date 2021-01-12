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

package org.apache.cxf.ws.policy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.Assertion;

/**
 *
 */
public class PolicyVerificationInInterceptor extends AbstractPolicyInterceptor {
    public static final PolicyVerificationInInterceptor INSTANCE = new PolicyVerificationInInterceptor();

    private static final Logger LOG = LogUtils.getL7dLogger(PolicyVerificationInInterceptor.class);

    private static boolean ignoreUnsupportedPolicy; // Liberty change: line added

    // Liberty change: static block below is added
    static {
        String skipPolicyCheck = System.getProperty("cxf.ignore.unsupported.policy");
        LOG.log(Level.FINE, "cxf.ignore.unsupported.policy property is set to " + skipPolicyCheck);

        if (skipPolicyCheck != null
            && skipPolicyCheck.trim().length() > 0
            && skipPolicyCheck.trim().equalsIgnoreCase("true")) {
            ignoreUnsupportedPolicy = true;
        }
    } // Liberty change: end

    public PolicyVerificationInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    /**
     * Determines the effective policy, and checks if one of its alternatives
     * is supported.
     *
     * @param message
     * @throws PolicyException if none of the alternatives is supported
     */
    protected void handle(Message message) {

        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }

        Exchange exchange = message.getExchange();
        BindingOperationInfo boi = exchange.getBindingOperationInfo();
        if (null == boi) {
            LOG.fine("No binding operation info.");
            return;
        }

        Endpoint e = exchange.getEndpoint();
        if (null == e) {
            LOG.fine("No endpoint.");
            return;
        }

        Bus bus = exchange.getBus();
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }

        if (MessageUtils.isPartialResponse(message)) {
            LOG.fine("Not verifying policies on inbound partial response.");
            return;
        }

        getTransportAssertions(message);

        EffectivePolicy effectivePolicy = message.get(EffectivePolicy.class);
        if (effectivePolicy == null) {
            EndpointInfo ei = e.getEndpointInfo();
            if (MessageUtils.isRequestor(message)) {
                effectivePolicy = pe.getEffectiveClientResponsePolicy(ei, boi, message);
            } else {
                effectivePolicy = pe.getEffectiveServerRequestPolicy(ei, boi, message);
            }
        }
        try {
            // List<List<Assertion>> usedAlternatives = aim.checkEffectivePolicy(effectivePolicy.getPolicy());  Liberty change: line removed
            // Liberty change: 7 lines below are added
            List<List<Assertion>> usedAlternatives = null;
            if (ignoreUnsupportedPolicy) {
                LOG.fine("WARNING: checkEffectivePolicy will not be called because "
                    + "property cxf.ignore.unsupported.policy is set to true.");
            } else {
                usedAlternatives = aim.checkEffectivePolicy(effectivePolicy.getPolicy());
            } // Liberty change: end

            if (usedAlternatives != null && !usedAlternatives.isEmpty() && message.getExchange() != null) {
                message.getExchange().put("ws-policy.validated.alternatives", usedAlternatives);
            }
        } catch (PolicyException ex) {
            LOG.log(Level.SEVERE, "Inbound policy verification failed: " + ex.getMessage());
            //To check if there is ws addressing policy violation and throw WSA specific
            //exception to pass jaxws2.2 tests
            if (ex.getMessage().indexOf("Addressing") > -1) {
                throw new Fault("A required header representing a Message Addressing Property "
                                    + "is not present", LOG)
                    .setFaultCode(new QName("http://www.w3.org/2005/08/addressing",
                                              "MessageAddressingHeaderRequired"));
            }
            throw ex;
        }
        LOG.fine("Verified policies for inbound message.");
    }

}
