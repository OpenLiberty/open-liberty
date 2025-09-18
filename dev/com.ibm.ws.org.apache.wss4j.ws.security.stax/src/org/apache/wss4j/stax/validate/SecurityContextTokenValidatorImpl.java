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
package org.apache.wss4j.stax.validate;

import org.apache.wss4j.binding.wssc.AbstractSecurityContextTokenType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.impl.securityToken.AbstractInboundSecurityToken;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

public class SecurityContextTokenValidatorImpl implements SecurityContextTokenValidator {

    @Override
    public InboundSecurityToken validate(final AbstractSecurityContextTokenType securityContextTokenType,
                                                 final String identifier, final TokenContext tokenContext)
            throws WSSecurityException {

        AbstractInboundSecurityToken securityContextToken = new AbstractInboundSecurityToken(
                tokenContext.getWsSecurityContext(), identifier,
                WSSecurityTokenConstants.KEYIDENTIFIER_EXTERNAL_REFERENCE, true) {

            @Override
            public boolean isAsymmetric() {
                return false;
            }

            @Override
            public Key getKey(String algorithmURI, XMLSecurityConstants.AlgorithmUsage algorithmUsage,
                              String correlationID) throws XMLSecurityException {

                Key key = getSecretKey().get(algorithmURI);
                if (key != null) {
                    return key;
                }

                WSPasswordCallback passwordCallback = new WSPasswordCallback(
                        identifier, WSPasswordCallback.SECURITY_CONTEXT_TOKEN);
                WSSUtils.doSecretKeyCallback(
                        tokenContext.getWssSecurityProperties().getCallbackHandler(), passwordCallback, null);
                if (passwordCallback.getKey() == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE,
                            "noKey", new Object[] {securityContextTokenType.getId()});
                }
                String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
                key = new SecretKeySpec(passwordCallback.getKey(), keyAlgorithm);
                setSecretKey(algorithmURI, key);
                return key;
            }

            @Override
            public WSSecurityTokenConstants.TokenType getTokenType() {
                return WSSecurityTokenConstants.SECURITY_CONTEXT_TOKEN;
            }
        };

        securityContextToken.setElementPath(tokenContext.getElementPath());
        securityContextToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());

        return securityContextToken;
    }
}
