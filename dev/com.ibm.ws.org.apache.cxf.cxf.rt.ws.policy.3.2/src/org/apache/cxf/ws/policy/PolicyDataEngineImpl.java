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
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.policy.PolicyCalculator;
import org.apache.cxf.policy.PolicyDataEngine;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.neethi.Assertion;

import com.ibm.websphere.ras.annotation.Trivial;

// Liberty Change
@Trivial  // Liberty change: line added
public class PolicyDataEngineImpl implements PolicyDataEngine {
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyDataEngineImpl.class);
    private Bus bus;
    private PolicyEngine policyEngine;

    public PolicyDataEngineImpl(Bus bus) {
        this.bus = bus;
    }

    // Liberty change: method below is added
    public PolicyDataEngineImpl() {
    } // Liberty change: end

    void setPolicyEngine(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    private PolicyEngine getPolicyEngine() {
        if (this.policyEngine == null) {
            this.policyEngine = bus.getExtension(PolicyEngine.class);
        }
        return this.policyEngine;
    }

    public <T> T getClientEndpointPolicy(Message m, EndpointInfo ei, Conduit c,
                                         PolicyCalculator<T> policyCalculator) {
        Collection<Assertion> alternative = getPolicyEngine().getClientEndpointPolicy(ei, c, m)
            .getChosenAlternative();
        List<T> filteredPolicies = new ArrayList<>();
        for (Assertion a : alternative) {
            if (policyCalculator.getDataClassName().equals(a.getName())) {
                T p = JaxbAssertion.cast(a, policyCalculator.getDataClass()).getData();
                filteredPolicies.add(p);
            }
        }
        return getPolicy(filteredPolicies, policyCalculator);
    }

    public <T> T getPolicy(Message message, T confPolicy, PolicyCalculator<T> intersector) {
        List<T> policies = getPoliciesFromMessage(intersector.getDataClassName(),
                                                  message, intersector.getDataClass());
        if (!policies.contains(confPolicy)) {
            policies.add(confPolicy);
        }
        return getPolicy(policies, intersector);
    }

    public <T> T getServerEndpointPolicy(Message m, EndpointInfo ei, Destination d,
                                         PolicyCalculator<T> policyCalculator) {
        Collection<Assertion> alternative = getPolicyEngine()
            .getServerEndpointPolicy(ei, d, m).getChosenAlternative();
        List<T> filteredPolicies = new ArrayList<>();
        for (Assertion a : alternative) {
            if (policyCalculator.getDataClassName().equals(a.getName())) {
                T p = JaxbAssertion.cast(a, policyCalculator.getDataClass()).getData();
                filteredPolicies.add(p);
            }
        }
        return getPolicy(filteredPolicies, policyCalculator);
    }

    private <T> List<T> getPoliciesFromMessage(QName name, Message message, Class<T> dataClass) {
        List<T> policies = new ArrayList<>();
        AssertionInfoMap amap = message.get(AssertionInfoMap.class);
        if (null == amap || amap.isEmpty()) {
            return policies;
        }
        Collection<AssertionInfo> ais = amap.get(name);
        if (null == ais) {
            return policies;
        }
        for (AssertionInfo ai : ais) {
            T policy = JaxbAssertion.cast(ai.getAssertion(), dataClass).getData();
            policies.add(policy);
        }
        return policies;
    }

    public <T> void assertMessage(Message message, T confPol,
                                  PolicyCalculator<T> policyCalculator) {
        T messagePol = message.get(policyCalculator.getDataClass());
        final T refPolicy = (messagePol != null) ? policyCalculator.intersect(messagePol, confPol) : confPol;

        AssertionInfoMap amap = message.get(AssertionInfoMap.class);
        if (null == amap || amap.isEmpty()) {
            return;
        }
        Collection<AssertionInfo> ais = amap.get(policyCalculator.getDataClassName());
        if (ais == null) {
            return;
        }
        for (AssertionInfo ai : ais) {
            T policy = JaxbAssertion.cast(ai.getAssertion(), policyCalculator.getDataClass()).getData();
            ai.setAsserted(policyCalculator.isAsserted(message, policy, refPolicy));
        }
    }

    private <T> T getPolicy(List<T> policies, PolicyCalculator<T> intersector) {
        T compatible = null;
        for (T p : policies) {
            if (null == compatible) {
                compatible = p;
            } else if (compatible != p) {
                compatible = intersector.intersect(p, compatible);
                if (null == compatible) {
                    logAndThrowPolicyException(p);
                }
            }
        }
        return compatible;
    }

    private <T> void logAndThrowPolicyException(T dataClass) {
        org.apache.cxf.common.i18n.Message msg =
            new org.apache.cxf.common.i18n.Message("INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS",
                                                   LOG, dataClass.getClass());
        LOG.severe(msg.toString());
        throw new PolicyException(msg);
    }

}
