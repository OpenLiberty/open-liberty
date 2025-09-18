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

package org.apache.wss4j.dom.action;

import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SecurityActionToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;

public class EncryptionAction implements Action {
    public void execute(WSHandler handler, SecurityActionToken actionToken, RequestData reqData)
            throws WSSecurityException {
        WSSecEncrypt wsEncrypt = new WSSecEncrypt(reqData.getSecHeader());
        wsEncrypt.setIdAllocator(reqData.getWssConfig().getIdAllocator());
        wsEncrypt.setWsDocInfo(reqData.getWsDocInfo());
        wsEncrypt.setExpandXopInclude(reqData.isExpandXopInclude());

        EncryptionActionToken encryptionToken = null;
        if (actionToken instanceof EncryptionActionToken) {
            encryptionToken = (EncryptionActionToken)actionToken;
        }
        if (encryptionToken == null) {
            encryptionToken = reqData.getEncryptionToken();
        }

        if (encryptionToken.getKeyIdentifierId() != 0) {
            wsEncrypt.setKeyIdentifierType(encryptionToken.getKeyIdentifierId());
        }

        if (encryptionToken.getSymmetricAlgorithm() != null) {
            wsEncrypt.setSymmetricEncAlgorithm(encryptionToken.getSymmetricAlgorithm());
        }
        if (encryptionToken.getKeyTransportAlgorithm() != null) {
            wsEncrypt.setKeyEncAlgo(encryptionToken.getKeyTransportAlgorithm());
        }
        if (encryptionToken.getDigestAlgorithm() != null) {
            wsEncrypt.setDigestAlgorithm(encryptionToken.getDigestAlgorithm());
        }

        if (encryptionToken.getMgfAlgorithm() != null) {
            wsEncrypt.setMGFAlgorithm(encryptionToken.getMgfAlgorithm());
        }

        wsEncrypt.setIncludeEncryptionToken(encryptionToken.isIncludeToken());

        wsEncrypt.setUserInfo(encryptionToken.getUser());
        wsEncrypt.setUseThisCert(encryptionToken.getCertificate());
        Crypto crypto = encryptionToken.getCrypto();
        boolean enableRevocation = Boolean.valueOf(handler.getStringOption(WSHandlerConstants.ENABLE_REVOCATION));
        if (enableRevocation && crypto != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(encryptionToken.getUser());
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs != null && certs.length > 0) {
                crypto.verifyTrust(certs, enableRevocation, null, null);
            }
        }
        if (!encryptionToken.getParts().isEmpty()) {
            wsEncrypt.getParts().addAll(encryptionToken.getParts());
        }

        wsEncrypt.setEncryptSymmKey(encryptionToken.isEncSymmetricEncryptionKey());

        byte[] ephemeralKey = encryptionToken.getKey();
        if (encryptionToken.isGetSymmetricKeyFromCallbackHandler()
            || !encryptionToken.isEncSymmetricEncryptionKey() && ephemeralKey == null) {
            CallbackHandler callbackHandler =
                handler.getPasswordCallbackHandler(reqData);
            // Get secret key for encryption from a CallbackHandler
            WSPasswordCallback pwcb =
                new WSPasswordCallback(encryptionToken.getUser(), WSPasswordCallback.SECRET_KEY);
            pwcb.setAlgorithm(wsEncrypt.getSymmetricEncAlgorithm());
            try {
                callbackHandler.handle(new Callback[] {pwcb});
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                        "empty", new Object[] {"WSHandler: password callback failed"});
            }

            ephemeralKey = pwcb.getKey();
            wsEncrypt.setCustomEKKeyInfoElement(pwcb.getKeyInfoReference());
        }

        SecretKey symmetricKey = null;
        if (ephemeralKey != null) {
            symmetricKey = KeyUtils.prepareSecretKey(wsEncrypt.getSymmetricEncAlgorithm(), ephemeralKey);
        } else {
            KeyGenerator keyGen = KeyUtils.getKeyGenerator(wsEncrypt.getSymmetricEncAlgorithm());
            symmetricKey = keyGen.generateKey();
        }

        if (encryptionToken.getTokenId() != null) {
            wsEncrypt.setEncKeyId(encryptionToken.getTokenId());
        }
        if (encryptionToken.getTokenType() != null) {
            wsEncrypt.setCustomReferenceValue(encryptionToken.getTokenType());
        }

        wsEncrypt.setAttachmentCallbackHandler(reqData.getAttachmentCallbackHandler());
        wsEncrypt.setStoreBytesInAttachment(reqData.isStoreBytesInAttachment());

        try {
            wsEncrypt.build(encryptionToken.getCrypto(), symmetricKey);
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "empty",
                                          new Object[] {"Error during encryption: "});
        }
    }
}
