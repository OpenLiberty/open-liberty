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

package org.apache.wss4j.dom.processor;

import java.time.Instant;
import java.util.List;

import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.w3c.dom.Element;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.utils.XMLUtils;

public class UsernameTokenProcessor implements Processor {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(UsernameTokenProcessor.class);

    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data
    ) throws WSSecurityException {
        // See if the token has been previously processed
        String id = elem.getAttributeNS(WSConstants.WSU_NS, "Id");
        if (!"".equals(id)) {
            Element foundElement = data.getWsDocInfo().getTokenElement(id);
            if (elem.equals(foundElement)) {
                WSSecurityEngineResult result = data.getWsDocInfo().getResult(id);
                return java.util.Collections.singletonList(result);
            } else if (foundElement != null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                );
            }
        }

        Validator validator = data.getValidator(WSConstants.USERNAME_TOKEN);
        Credential credential = handleUsernameToken(elem, validator, data);
        UsernameToken token = credential.getUsernametoken();

        int action = WSConstants.UT;
        byte[] secretKey = null;
        if (token.getPassword() == null) {
            action = WSConstants.UT_NOPASSWORD;
            if (token.isDerivedKey()) {
                String rawPassword =
                    UsernameTokenUtil.getRawPassword(data.getCallbackHandler(), token.getName(),
                                                     token.getPassword(), token.getPasswordType());
                secretKey = token.getDerivedKey(data.getBSPEnforcer(), rawPassword);
            }
        }
        WSSecurityEngineResult result = new WSSecurityEngineResult(action, token);
        String tokenId = token.getID();
        if (!"".equals(tokenId)) {
            result.put(WSSecurityEngineResult.TAG_ID, tokenId);
        }
        result.put(WSSecurityEngineResult.TAG_SECRET, secretKey);

        if (validator != null) {
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            if (credential.getTransformedToken() != null) {
                result.put(
                    WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN, credential.getTransformedToken()
                );
                if (credential.getPrincipal() != null) {
                    result.put(WSSecurityEngineResult.TAG_PRINCIPAL, credential.getPrincipal());
                } else {
                    SAMLTokenPrincipalImpl samlPrincipal =
                        new SAMLTokenPrincipalImpl(credential.getTransformedToken());
                    result.put(WSSecurityEngineResult.TAG_PRINCIPAL, samlPrincipal);
                }
            } else if (credential.getPrincipal() != null) {
                result.put(WSSecurityEngineResult.TAG_PRINCIPAL, credential.getPrincipal());
            } else {
                WSUsernameTokenPrincipalImpl principal =
                    new WSUsernameTokenPrincipalImpl(token.getName(), token.isHashed());
                if (token.getNonce() != null) {
                    principal.setNonce(XMLUtils.decode(token.getNonce()));
                }
                principal.setPassword(token.getPassword());
                principal.setCreatedTime(token.getCreated());
                principal.setPasswordType(token.getPasswordType());
                result.put(WSSecurityEngineResult.TAG_PRINCIPAL, principal);
            }
            result.put(WSSecurityEngineResult.TAG_SUBJECT, credential.getSubject());
        }

        data.getWsDocInfo().addTokenElement(elem);
        data.getWsDocInfo().addResult(result);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Check the UsernameToken element and validate it.
     *
     * @param token the DOM element that contains the UsernameToken
     * @param data The RequestData object from which to obtain configuration
     * @return a Credential object corresponding to the (validated) Username Token
     * @throws WSSecurityException
     */
    private Credential
    handleUsernameToken(
        Element token,
        Validator validator,
        RequestData data
    ) throws WSSecurityException {
        boolean allowNamespaceQualifiedPasswordTypes = data.isAllowNamespaceQualifiedPasswordTypes();
        int utTTL = data.getUtTTL();
        int futureTimeToLive = data.getUtFutureTTL();

        //
        // Parse and validate the UsernameToken element
        //
        UsernameToken ut =
            new UsernameToken(token, allowNamespaceQualifiedPasswordTypes, data.getBSPEnforcer());

        // Validate whether the security semantics have expired
        if (!ut.verifyCreated(utTTL, futureTimeToLive)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.MESSAGE_EXPIRED);
        }

        // Test for replay attacks
        ReplayCache replayCache = data.getNonceReplayCache();
        if (replayCache != null && ut.getNonce() != null) {
            if (replayCache.contains(ut.getNonce())) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "badUsernameToken",
                    new Object[] {"A replay attack has been detected"}
                );
            }

            // If no Created, then just cache for the default time
            // Otherwise, cache for the configured TTL of the UsernameToken Created time, as any
            // older token will just get rejected anyway
            Instant created = ut.getCreatedDate();
            //if (created == null || utTTL <= 0) {
            if (created == null || utTTL <= 0) {
            	// Liberty Change
            	//replayCache.add(ut.getNonce());
            	if (utTTL <= 0) {
            		replayCache.add(ut.getNonce());
            	} else { 
            		replayCache.add(ut.getNonce(), Instant.now().plusSeconds(utTTL));
            	}
            	// End Liberty Change
            } else {
                replayCache.add(ut.getNonce(), Instant.now().plusSeconds(utTTL));
            }
        }

        Credential credential = new Credential();
        credential.setUsernametoken(ut);
        if (validator != null) {
            return validator.validate(credential, data);
        }
        return credential;
    }

}
