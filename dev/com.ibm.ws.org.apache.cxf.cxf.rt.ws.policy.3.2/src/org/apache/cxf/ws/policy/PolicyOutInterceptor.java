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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

/**
 *
 */
public class PolicyOutInterceptor extends AbstractPolicyInterceptor {
    public static final PolicyOutInterceptor INSTANCE = new PolicyOutInterceptor();

    private static final Logger LOG = LogUtils.getL7dLogger(PolicyOutInterceptor.class);

    public PolicyOutInterceptor() {
        super(PolicyConstants.POLICY_OUT_INTERCEPTOR_ID, Phase.SETUP);
    }

    protected void handle(Message msg) {
        Exchange exchange = msg.getExchange();
        Bus bus = exchange.getBus();

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
        EndpointInfo ei = e.getEndpointInfo();

        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }

        Collection<Assertion> assertions = new ArrayList<>();

        // 1. Check overridden policy
        Policy p = (Policy)msg.getContextualProperty(PolicyConstants.POLICY_OVERRIDE);
        if (p != null) {
            EndpointPolicyImpl endpi = new EndpointPolicyImpl(p);
            EffectivePolicyImpl effectivePolicy = new EffectivePolicyImpl();
            effectivePolicy.initialise(endpi, pe, false, msg);
            msg.put(EffectivePolicy.class, effectivePolicy);
            PolicyUtils.logPolicy(LOG, Level.FINEST, "Using effective policy: ",
                                  effectivePolicy.getPolicy());

            addInterceptors(effectivePolicy.getInterceptors(), msg);
            assertions.addAll(effectivePolicy.getChosenAlternative());
        } else if (MessageUtils.isRequestor(msg)) {
            // 2. Process client policy
            Conduit conduit = exchange.getConduit(msg);

            // add the required interceptors
            EffectivePolicy effectivePolicy = pe.getEffectiveClientRequestPolicy(ei, boi, conduit, msg);
            msg.put(EffectivePolicy.class, effectivePolicy);
            if (effectivePolicy != null) {
                PolicyUtils.logPolicy(
                    LOG, Level.FINEST, "Using effective policy: ", effectivePolicy.getPolicy()
                );
                addInterceptors(effectivePolicy.getInterceptors(), msg);
                assertions.addAll(effectivePolicy.getChosenAlternative());
            }
        } else {
            // 3. Process server policy
            Destination destination = exchange.getDestination();
            List<List<Assertion>> incoming
                = CastUtils.cast((List<?>)exchange.get("ws-policy.validated.alternatives"));
            EffectivePolicy effectivePolicy
                = pe.getEffectiveServerResponsePolicy(ei, boi, destination, incoming, msg);
            msg.put(EffectivePolicy.class, effectivePolicy);
            if (effectivePolicy != null) {
                PolicyUtils.logPolicy(
                    LOG, Level.FINEST, "Using effective policy: ", effectivePolicy.getPolicy()
                );
                addInterceptors(effectivePolicy.getInterceptors(), msg);
                assertions.addAll(effectivePolicy.getChosenAlternative());
            }
        }

        // insert assertions of endpoint's fault vocabulary into message
        if (!assertions.isEmpty()) {
            if (LOG.isLoggable(Level.FINEST)) {
                StringBuilder buf = new StringBuilder();
                buf.append("Chosen alternative: ");
                String nl = SystemPropertyAction.getProperty("line.separator");
                buf.append(nl);
                for (Assertion a : assertions) {
                    PolicyUtils.printPolicyComponent(a, buf, 1);
                }
                LOG.finest(buf.toString());
            }
            msg.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
            msg.getInterceptorChain().add(PolicyVerificationOutInterceptor.INSTANCE);
        }
    }

    private static void addInterceptors(List<Interceptor<? extends Message>> interceptors, Message msg) {
        for (Interceptor<? extends Message> oi : interceptors) {
            msg.getInterceptorChain().add(oi);
            LOG.log(Level.FINE, "Added interceptor of type {0}", oi.getClass().getSimpleName());
        }
    }
}
