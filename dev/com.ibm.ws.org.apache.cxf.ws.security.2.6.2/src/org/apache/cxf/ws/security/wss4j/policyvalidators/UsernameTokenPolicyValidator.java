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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate a UsernameToken policy.
 */
public class UsernameTokenPolicyValidator 
    extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.USERNAME_TOKEN);
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
        List<WSSecurityEngineResult> utResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT, utResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_NOPASSWORD, utResults);
        
        for (AssertionInfo ai : ais) {
            org.apache.cxf.ws.security.policy.model.UsernameToken usernameTokenPolicy = 
                (org.apache.cxf.ws.security.policy.model.UsernameToken)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(usernameTokenPolicy, message)) {
                continue;
            }

            if (utResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            if (!checkTokens(usernameTokenPolicy, ai, utResults)) {
                continue;
            }
        }
        return true;
    }
    
    /**
     * All UsernameTokens must conform to the policy
     */
    public boolean checkTokens(
        org.apache.cxf.ws.security.policy.model.UsernameToken usernameTokenPolicy,
        AssertionInfo ai,
        List<WSSecurityEngineResult> utResults
    ) {
        for (WSSecurityEngineResult result : utResults) {
            UsernameToken usernameToken = 
                (UsernameToken)result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);
            if (usernameTokenPolicy.isHashPassword() != usernameToken.isHashed()) {
                ai.setNotAsserted("Password hashing policy not enforced");
                return false;
            }
            if (usernameTokenPolicy.isNoPassword() && (usernameToken.getPassword() != null)) {
                ai.setNotAsserted("Username Token NoPassword policy not enforced");
                return false;
            } else if (!usernameTokenPolicy.isNoPassword() && (usernameToken.getPassword() == null)
                && isNonEndorsingSupportingToken(usernameTokenPolicy)) {
                ai.setNotAsserted("Username Token No Password supplied");
                return false;
            }
            
            if (usernameTokenPolicy.isRequireCreated() 
                && (usernameToken.getCreated() == null || usernameToken.isHashed())) {
                ai.setNotAsserted("Username Token Created policy not enforced");
                return false;
            }
            if (usernameTokenPolicy.isRequireNonce() 
                && (usernameToken.getNonce() == null || usernameToken.isHashed())) {
                ai.setNotAsserted("Username Token Nonce policy not enforced");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Return true if this UsernameToken policy is a (non-endorsing)SupportingToken. If this is
     * true then the corresponding UsernameToken must have a password element.
     */
    private boolean isNonEndorsingSupportingToken(
        org.apache.cxf.ws.security.policy.model.UsernameToken usernameTokenPolicy
    ) {
        SupportingToken supportingToken = usernameTokenPolicy.getSupportingToken();
        if (supportingToken != null) {
            SPConstants.SupportTokenType type = supportingToken.getTokenType();
            if (type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SUPPORTING
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED_ENCRYPTED
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_ENCRYPTED) {
                return true;
            }
        }
        return false;
    }
    
}