/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.acs;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.security.SecurityPolicy;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.ws.security.SecurityPolicyRule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;

/**
 * Basic security policy implementation which evaluates a given set of {@link SecurityPolicyRule} in an ordered manner.
 *
 * A policy evaluates successfully if, and only if, all policy rules evaluate successfully.
 */
public class AcsSecurityPolicy implements SecurityPolicy {
    private static TraceComponent tc = Tr.register(AcsSecurityPolicy.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    /** Registered security rules. */
    final ArrayList<SecurityPolicyRule> rules;

    /** Constructor. */
    public AcsSecurityPolicy() {
        rules = new ArrayList<SecurityPolicyRule>(5);
    }

    public boolean add(SecurityPolicyRule securityPolicyRule) {
        //rules.add(new BaseSAMLSimpleSignatureSecurityPolicyRule());){
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "add SecurityPolicyRule:" + securityPolicyRule);
        }
        return rules.add(securityPolicyRule);
    }

    /** {@inheritDoc} */
    @Override
    public List<SecurityPolicyRule> getPolicyRules() {
        return rules;
    }

    /** {@inheritDoc} */
    @Override
    public void evaluate(MessageContext messageContext) throws SecurityPolicyException {
        for (SecurityPolicyRule rule : getPolicyRules()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "beginning SecurityPolicyRule evaluate:" + rule);
            }
            rule.evaluate(messageContext);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SecurityPolicyRule evaluate successfully:" + rule);
            }
        }
    }
}