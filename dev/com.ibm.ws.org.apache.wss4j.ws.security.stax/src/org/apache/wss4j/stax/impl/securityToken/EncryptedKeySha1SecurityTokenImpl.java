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
package org.apache.wss4j.stax.impl.securityToken;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSInboundSecurityContext;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityToken.EncryptedKeySha1SecurityToken;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.impl.securityToken.AbstractInboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;

public class EncryptedKeySha1SecurityTokenImpl
        extends AbstractInboundSecurityToken implements EncryptedKeySha1SecurityToken {

    private CallbackHandler callbackHandler;

    public EncryptedKeySha1SecurityTokenImpl(
            WSInboundSecurityContext inboundSecurityContext, CallbackHandler callbackHandler,
            String sha1Identifier, String id) {

        super(inboundSecurityContext, id, WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER, false);
        this.callbackHandler = callbackHandler;
        setSha1Identifier(sha1Identifier);
    }

    @Override
    public boolean isAsymmetric() throws XMLSecurityException {
        return false;
    }

    @Override
    protected Key getKey(String algorithmURI, XMLSecurityConstants.AlgorithmUsage algorithmUsage,
                         String correlationID) throws XMLSecurityException {

        Key key = getSecretKey().get(algorithmURI);
        if (key != null) {
            return key;
        }

        WSPasswordCallback secretKeyCallback =
                new WSPasswordCallback(getSha1Identifier(), null,
                        WSSConstants.NS_ENCRYPTED_KEY_SHA1, WSPasswordCallback.SECRET_KEY);
        WSSUtils.doSecretKeyCallback(callbackHandler, secretKeyCallback, getSha1Identifier());
        if (secretKeyCallback.getKey() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noKey",
                                          new Object[] {getSha1Identifier()});
        }

        String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
        key = new SecretKeySpec(secretKeyCallback.getKey(), keyAlgorithm);
        setSecretKey(algorithmURI, key);
        return key;
    }

    @Override
    public SecurityTokenConstants.TokenType getTokenType() {
        return WSSecurityTokenConstants.EncryptedKeyToken;
    }
}
