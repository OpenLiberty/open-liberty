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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;

import org.apache.wss4j.common.SignatureEncryptionActionToken;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.WSSecDerivedKeyBase;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.xml.security.stax.impl.util.IDGenerator;

public abstract class AbstractDerivedAction {

    protected Node findEncryptedKeySibling(RequestData reqData) {
        Element secHeader = reqData.getSecHeader().getSecurityHeaderElement();
        return findSibling(secHeader, WSConstants.ENC_NS, "EncryptedKey");
    }

    protected Node findSCTSibling(RequestData reqData) {
        String namespace = ConversationConstants.WSC_NS_05_12;
        if (!reqData.isUse200512Namespace()) {
            namespace = ConversationConstants.WSC_NS_05_02;
        }
        Element secHeader = reqData.getSecHeader().getSecurityHeaderElement();
        return findSibling(secHeader, namespace, "SecurityContextToken");
    }

    protected Node findSibling(Element secHeader, String namespace, String localName) {
        if (secHeader == null) {
            return null;
        }
        Node firstChild = secHeader.getFirstChild();
        while (firstChild != null) {
            if (firstChild instanceof Element
                && namespace.equals(((Element)firstChild).getNamespaceURI())
                && localName.equals(((Element)firstChild).getLocalName())
                && firstChild.getNextSibling() != null) {
                return firstChild.getNextSibling();
            }
            firstChild = firstChild.getNextSibling();
        }
        return null;
    }

    // Use a previous derived action which has already set up a SecurityContextToken
    protected void setupSCTReference(WSSecDerivedKeyBase derivedKeyBase,
                                        SignatureEncryptionActionToken previousActionToken,
                                        boolean use200512Namespace) throws WSSecurityException {
        if (use200512Namespace) {
            derivedKeyBase.setCustomValueType(WSConstants.WSC_SCT_05_12);
        } else {
            derivedKeyBase.setCustomValueType(WSConstants.WSC_SCT);
        }

        String tokenIdentifier = previousActionToken.getKeyIdentifier();
        derivedKeyBase.setTokenIdentifier(tokenIdentifier);
    }

    protected Element setupSCTReference(WSSecDerivedKeyBase derivedKeyBase,
                                        WSPasswordCallback passwordCallback,
                                        SignatureEncryptionActionToken actionToken,
                                        boolean use200512Namespace,
                                        Document doc) throws WSSecurityException {
        if (use200512Namespace) {
            derivedKeyBase.setCustomValueType(WSConstants.WSC_SCT_05_12);
        } else {
            derivedKeyBase.setCustomValueType(WSConstants.WSC_SCT);
        }

        String tokenIdentifier = IDGenerator.generateID("uuid:");
        derivedKeyBase.setTokenIdentifier(tokenIdentifier);

        actionToken.setKey(passwordCallback.getKey());
        actionToken.setKeyIdentifier(tokenIdentifier);

        int version = ConversationConstants.VERSION_05_12;
        if (!use200512Namespace) {
            version = ConversationConstants.VERSION_05_02;
        }

        SecurityContextToken sct = new SecurityContextToken(version, doc, tokenIdentifier);
        return sct.getElement();
    }

    // Use a previous derived action which has already set up an EncryptedKey
    protected void setupEKReference(WSSecDerivedKeyBase derivedKeyBase,
                                        SignatureEncryptionActionToken previousActionToken) throws WSSecurityException {
        derivedKeyBase.setCustomValueType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);

        String tokenIdentifier = previousActionToken.getKeyIdentifier();
        derivedKeyBase.setTokenIdentifier(tokenIdentifier);
    }

    protected Element setupEKReference(WSSecDerivedKeyBase derivedKeyBase,
                                       WSSecHeader securityHeader,
                                        WSPasswordCallback passwordCallback,
                                        SignatureEncryptionActionToken actionToken,
                                        boolean use200512Namespace,
                                        Document doc,
                                        String keyTransportAlgorithm,
                                        String mgfAlgorithm,
                                        SecretKey symmetricKey) throws WSSecurityException {
        derivedKeyBase.setCustomValueType(WSConstants.WSS_ENC_KEY_VALUE_TYPE);

        WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey(securityHeader);
        encrKeyBuilder.setUserInfo(actionToken.getUser());
        if (actionToken.getDerivedKeyIdentifier() != 0) {
            encrKeyBuilder.setKeyIdentifierType(actionToken.getDerivedKeyIdentifier());
        } else {
            encrKeyBuilder.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
        }

        if (actionToken.getDigestAlgorithm() != null) {
            encrKeyBuilder.setDigestAlgorithm(actionToken.getDigestAlgorithm());
        }
        if (keyTransportAlgorithm != null) {
            encrKeyBuilder.setKeyEncAlgo(keyTransportAlgorithm);
        }
        if (mgfAlgorithm != null) {
            encrKeyBuilder.setMGFAlgorithm(mgfAlgorithm);
        }
        encrKeyBuilder.prepare(actionToken.getCrypto(), symmetricKey);

        String tokenIdentifier = encrKeyBuilder.getId();

        actionToken.setKey(symmetricKey.getEncoded());
        actionToken.setKeyIdentifier(tokenIdentifier);
        derivedKeyBase.setTokenIdentifier(tokenIdentifier);

        return encrKeyBuilder.getEncryptedKeyElement();
    }
}
