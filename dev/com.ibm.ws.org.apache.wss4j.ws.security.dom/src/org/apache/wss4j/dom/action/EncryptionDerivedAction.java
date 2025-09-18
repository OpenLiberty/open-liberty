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

import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SecurityActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.message.WSSecDKEncrypt;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class EncryptionDerivedAction extends AbstractDerivedAction implements Action {

    public void execute(WSHandler handler, SecurityActionToken actionToken, RequestData reqData)
            throws WSSecurityException {
        CallbackHandler callbackHandler = reqData.getCallbackHandler();
        if (callbackHandler == null) {
            callbackHandler = handler.getPasswordCallbackHandler(reqData);
        }

        EncryptionActionToken encryptionToken = null;
        if (actionToken instanceof EncryptionActionToken) {
            encryptionToken = (EncryptionActionToken)actionToken;
        }
        if (encryptionToken == null) {
            encryptionToken = reqData.getEncryptionToken();
        }

        WSPasswordCallback passwordCallback =
            handler.getPasswordCB(encryptionToken.getUser(), WSConstants.DKT_ENCR, callbackHandler, reqData);
        WSSecDKEncrypt wsEncrypt = new WSSecDKEncrypt(reqData.getSecHeader());
        wsEncrypt.setIdAllocator(reqData.getWssConfig().getIdAllocator());
        wsEncrypt.setWsDocInfo(reqData.getWsDocInfo());
        wsEncrypt.setExpandXopInclude(reqData.isExpandXopInclude());

        if (encryptionToken.getKeyIdentifierId() != 0) {
            wsEncrypt.setKeyIdentifierType(encryptionToken.getKeyIdentifierId());
        }

        if (encryptionToken.getSymmetricAlgorithm() != null) {
            wsEncrypt.setSymmetricEncAlgorithm(encryptionToken.getSymmetricAlgorithm());
        }
        wsEncrypt.setUserInfo(encryptionToken.getUser(), passwordCallback.getPassword());

        if (reqData.isUse200512Namespace()) {
            wsEncrypt.setWscVersion(ConversationConstants.VERSION_05_12);
        } else {
            wsEncrypt.setWscVersion(ConversationConstants.VERSION_05_02);
        }

        if (encryptionToken.getDerivedKeyLength() > 0) {
            wsEncrypt.setDerivedKeyLength(encryptionToken.getDerivedKeyLength());
        }

        Document doc = reqData.getSecHeader().getSecurityHeaderElement().getOwnerDocument();
        String derivedKeyTokenReference = encryptionToken.getDerivedKeyTokenReference();
        Element tokenElement = null;
        SecretKey symmetricKey = null;
        if ("EncryptedKey".equals(derivedKeyTokenReference)) {
            if (reqData.getSignatureToken() == null || reqData.getSignatureToken().getKey() == null
                || reqData.getSignatureToken().getKeyIdentifier() == null) {
                String symmetricKeyAlgorithm = encryptionToken.getSymmetricAlgorithm();
                if (symmetricKeyAlgorithm == null) {
                    symmetricKeyAlgorithm = WSConstants.AES_128;
                }
                KeyGenerator keyGen = KeyUtils.getKeyGenerator(symmetricKeyAlgorithm);
                symmetricKey = keyGen.generateKey();
            }

            tokenElement = setupEncryptedKeyTokenReference(reqData, encryptionToken, wsEncrypt, passwordCallback, doc, symmetricKey);
        } else if ("SecurityContextToken".equals(derivedKeyTokenReference)) {
            tokenElement = setupSCTTokenReference(reqData, encryptionToken, wsEncrypt, passwordCallback, doc);
        }

        wsEncrypt.setAttachmentCallbackHandler(reqData.getAttachmentCallbackHandler());
        wsEncrypt.setStoreBytesInAttachment(reqData.isStoreBytesInAttachment());

        try {
            List<WSEncryptionPart> parts = encryptionToken.getParts();
            if (parts != null && !parts.isEmpty()) {
                wsEncrypt.getParts().addAll(parts);
            } else {
                wsEncrypt.getParts().add(WSSecurityUtil.getDefaultEncryptionPart(doc));
            }

            byte[] key = getKey(reqData.getSignatureToken(), passwordCallback, symmetricKey);
            wsEncrypt.prepare(key);

            Element externRefList = wsEncrypt.encrypt();

            // Put the DerivedKeyToken Element in the right place in the security header
            Node nextSibling = null;
            if (tokenElement == null
                && "EncryptedKey".equals(encryptionToken.getDerivedKeyTokenReference())) {
                nextSibling = findEncryptedKeySibling(reqData);
            } else if (tokenElement == null
                && "SecurityContextToken".equals(encryptionToken.getDerivedKeyTokenReference())) {
                nextSibling = findSCTSibling(reqData);
            }
            if (nextSibling == null) {
                wsEncrypt.prependDKElementToHeader();
            } else {
                reqData.getSecHeader().getSecurityHeaderElement().insertBefore(
                    wsEncrypt.getdktElement(), nextSibling);
            }

            // Add the ReferenceList to the security header
            wsEncrypt.addExternalRefElement(externRefList);

            if (tokenElement != null) {
                WSSecurityUtil.prependChildElement(reqData.getSecHeader().getSecurityHeaderElement(), tokenElement);
            }

            wsEncrypt.clean();
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                                          "empty", new Object[] {"Error during Encryption: "});
        }
    }

    private Element setupSCTTokenReference(
                                        RequestData reqData, EncryptionActionToken encryptionToken,
                                        WSSecDKEncrypt wsEncrypt, WSPasswordCallback passwordCallback,
                                        Document doc
        ) throws WSSecurityException {
        if (reqData.getSignatureToken() != null && reqData.getSignatureToken().getKey() != null
            && reqData.getSignatureToken().getKeyIdentifier() != null) {
            setupSCTReference(wsEncrypt, reqData.getSignatureToken(), reqData.isUse200512Namespace());
            return null;
        } else {
            return setupSCTReference(wsEncrypt, passwordCallback, encryptionToken, reqData.isUse200512Namespace(), doc);
        }
    }

    private Element setupEncryptedKeyTokenReference(
        RequestData reqData, EncryptionActionToken encryptionToken,
        WSSecDKEncrypt wsEncrypt, WSPasswordCallback passwordCallback,
        Document doc, SecretKey symmetricKey
    ) throws WSSecurityException {
        if (symmetricKey == null) {
            setupEKReference(wsEncrypt, reqData.getSignatureToken());
            return null;
        } else {
            return setupEKReference(wsEncrypt, reqData.getSecHeader(), passwordCallback, encryptionToken,
                                              reqData.isUse200512Namespace(), doc, null, null, symmetricKey);
        }
    }

    private byte[] getKey(SignatureActionToken encryptionToken,
                          WSPasswordCallback passwordCallback,
                          SecretKey symmetricKey) throws WSSecurityException {
        if (symmetricKey != null) {
            return symmetricKey.getEncoded();
        } else if (encryptionToken != null && encryptionToken.getKey() != null
            && encryptionToken.getKeyIdentifier() != null) {
            return encryptionToken.getKey();
        } else {
            return passwordCallback.getKey();
        }
    }
}
