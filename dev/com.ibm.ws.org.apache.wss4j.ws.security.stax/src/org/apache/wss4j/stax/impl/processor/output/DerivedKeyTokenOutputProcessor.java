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
package org.apache.wss4j.stax.impl.processor.output;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.common.derivedKey.AlgoFactory;
import org.apache.wss4j.common.derivedKey.DerivationAlgorithm;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.config.JCEAlgorithmMapper;
import org.apache.xml.security.stax.ext.AbstractOutputProcessor;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.impl.securityToken.GenericOutboundSecurityToken;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;
import org.apache.xml.security.utils.XMLUtils;

public class DerivedKeyTokenOutputProcessor extends AbstractOutputProcessor {

    public DerivedKeyTokenOutputProcessor() throws XMLSecurityException {
        super();
    }

    @Override
    public void processEvent(XMLSecEvent xmlSecEvent, OutputProcessorChain outputProcessorChain)
        throws XMLStreamException, XMLSecurityException {
        try {

            String tokenId = outputProcessorChain.getSecurityContext().get(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_DERIVED_KEY);
            if (tokenId == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
            }
            SecurityTokenProvider<OutboundSecurityToken> wrappingSecurityTokenProvider =
                outputProcessorChain.getSecurityContext().getSecurityTokenProvider(tokenId);
            if (wrappingSecurityTokenProvider == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
            }
            final OutboundSecurityToken wrappingSecurityToken = wrappingSecurityTokenProvider.getSecurityToken();
            if (wrappingSecurityToken == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
            }

            final String wsuIdDKT = IDGenerator.generateID(null);

            int offset = 0;
            int length = 0;

            XMLSecurityConstants.Action action = getAction();
            if (WSSConstants.SIGNATURE_WITH_DERIVED_KEY.equals(action)) {
                if (((WSSSecurityProperties)getSecurityProperties()).getDerivedSignatureKeyLength() > 0) {
                    length = ((WSSSecurityProperties)getSecurityProperties()).getDerivedSignatureKeyLength();
                } else {
                    length = JCEAlgorithmMapper.getKeyLengthFromURI(getSecurityProperties().getSignatureAlgorithm()) / 8;
                    if (length == 0) {
                        length = KeyUtils.getKeyLength(getSecurityProperties().getSignatureAlgorithm()) / 8;
                    }
                }
            } else if (WSSConstants.ENCRYPTION_WITH_DERIVED_KEY.equals(action)) {
                if (((WSSSecurityProperties)getSecurityProperties()).getDerivedEncryptionKeyLength() > 0) {
                    length = ((WSSSecurityProperties)getSecurityProperties()).getDerivedEncryptionKeyLength();
                } else {
                    length = JCEAlgorithmMapper.getKeyLengthFromURI(getSecurityProperties().getEncryptionSymAlgorithm()) / 8;
                    if (length == 0) {
                        length = KeyUtils.getKeyLength(getSecurityProperties().getEncryptionSymAlgorithm()) / 8;
                    }
                }
            }

            String defaultLabel =
                WSSConstants.WS_SEC_CONV_DEFAULT_LABEL + WSSConstants.WS_SEC_CONV_DEFAULT_LABEL;
            byte[] label = defaultLabel.getBytes(StandardCharsets.UTF_8);

            byte[] nonce = WSSConstants.generateBytes(16);

            byte[] seed = new byte[label.length + nonce.length];
            System.arraycopy(label, 0, seed, 0, label.length);
            System.arraycopy(nonce, 0, seed, label.length, nonce.length);

            DerivationAlgorithm derivationAlgorithm =
                AlgoFactory.getInstance(WSSConstants.P_SHA_1);

            byte[] secret;
            if (WSSecurityTokenConstants.SECURITY_CONTEXT_TOKEN.equals(wrappingSecurityToken.getTokenType())) {
                WSPasswordCallback passwordCallback = new WSPasswordCallback(wsuIdDKT, WSPasswordCallback.SECRET_KEY);
                WSSUtils.doSecretKeyCallback(((WSSSecurityProperties)securityProperties).getCallbackHandler(),
                                             passwordCallback, wsuIdDKT);
                if (passwordCallback.getKey() == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noKey",
                                                  new Object[] {wsuIdDKT});
                }
                secret = passwordCallback.getKey();
            } else {
                secret = wrappingSecurityToken.getSecretKey("").getEncoded();
            }

            final byte[] derivedKeyBytes = derivationAlgorithm.createKey(secret, seed, offset, length);

            final GenericOutboundSecurityToken derivedKeySecurityToken =
                    new GenericOutboundSecurityToken(wsuIdDKT, WSSecurityTokenConstants.DerivedKeyToken) {

                @Override
                public Key getSecretKey(String algorithmURI) throws WSSecurityException {

                    Key key = null;
                    try {
                        key = super.getSecretKey(algorithmURI);
                    } catch (XMLSecurityException e) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
                    }
                    if (key != null) {
                        return key;
                    }
                    String algoFamily = JCEAlgorithmMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
                    key = new SecretKeySpec(derivedKeyBytes, algoFamily);
                    setSecretKey(algorithmURI, key);
                    return key;
                }
            };

            derivedKeySecurityToken.setKeyWrappingToken(wrappingSecurityToken);
            wrappingSecurityToken.addWrappedToken(derivedKeySecurityToken);

            SecurityTokenProvider<OutboundSecurityToken> derivedKeysecurityTokenProvider =
                    new SecurityTokenProvider<OutboundSecurityToken>() {

                @Override
                public OutboundSecurityToken getSecurityToken() throws WSSecurityException {
                    return derivedKeySecurityToken;
                }

                @Override
                public String getId() {
                    return wsuIdDKT;
                }
            };

            if (WSSConstants.SIGNATURE_WITH_DERIVED_KEY.equals(action)) {
                outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, wsuIdDKT);
            } else if (WSSConstants.ENCRYPTION_WITH_DERIVED_KEY.equals(action)) {
                outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION, wsuIdDKT);
            }
            outputProcessorChain.getSecurityContext().registerSecurityTokenProvider(wsuIdDKT, derivedKeysecurityTokenProvider);
            FinalDerivedKeyTokenOutputProcessor finalDerivedKeyTokenOutputProcessor =
                    new FinalDerivedKeyTokenOutputProcessor(derivedKeySecurityToken, offset, length,
                                                            XMLUtils.encodeToString(nonce),
                                                            ((WSSSecurityProperties)getSecurityProperties()).isUse200512Namespace(),
                                                            wrappingSecurityToken.getSha1Identifier());
            finalDerivedKeyTokenOutputProcessor.setXMLSecurityProperties(getSecurityProperties());
            finalDerivedKeyTokenOutputProcessor.setAction(getAction(), getActionOrder());
            if (wrappingSecurityToken.getProcessor() != null) {
                finalDerivedKeyTokenOutputProcessor.addBeforeProcessor(wrappingSecurityToken.getProcessor().getClass());
            } else {
                finalDerivedKeyTokenOutputProcessor.addAfterProcessor(ReferenceListOutputProcessor.class);
            }
            finalDerivedKeyTokenOutputProcessor.init(outputProcessorChain);
            derivedKeySecurityToken.setProcessor(finalDerivedKeyTokenOutputProcessor);
        } finally {
            outputProcessorChain.removeProcessor(this);
        }
        outputProcessorChain.processEvent(xmlSecEvent);
    }

    static class FinalDerivedKeyTokenOutputProcessor extends AbstractOutputProcessor {

        private final OutboundSecurityToken securityToken;
        private final int offset;
        private final int length;
        private final String nonce;
        private final boolean use200512Namespace;
        private final String sha1Identifier;

        FinalDerivedKeyTokenOutputProcessor(OutboundSecurityToken securityToken, int offset,
                                            int length, String nonce, boolean use200512Namespace,
                                            String sha1Identifier) throws XMLSecurityException {

            super();
            this.securityToken = securityToken;
            this.offset = offset;
            this.length = length;
            this.nonce = nonce;
            this.use200512Namespace = use200512Namespace;
            this.sha1Identifier = sha1Identifier;
        }

        @Override
        public void processEvent(XMLSecEvent xmlSecEvent, OutputProcessorChain outputProcessorChain)
                throws XMLStreamException, XMLSecurityException {

            outputProcessorChain.processEvent(xmlSecEvent);

            if (WSSUtils.isSecurityHeaderElement(xmlSecEvent, ((WSSSecurityProperties) getSecurityProperties()).getActor())) {

                final QName headerElementName = getHeaderElementName();
                OutputProcessorUtils.updateSecurityHeaderOrder(outputProcessorChain, headerElementName, getAction(), false);

                OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);

                List<XMLSecAttribute> attributes = new ArrayList<>(1);
                attributes.add(createAttribute(WSSConstants.ATT_WSU_ID, securityToken.getId()));
                createStartElementAndOutputAsEvent(subOutputProcessorChain, headerElementName, true, attributes);

                createSecurityTokenReferenceStructureForDerivedKey(subOutputProcessorChain, securityToken,
                        ((WSSSecurityProperties) getSecurityProperties()).getDerivedKeyKeyIdentifier(),
                        ((WSSSecurityProperties) getSecurityProperties()).getDerivedKeyTokenReference(),
                        getSecurityProperties().isUseSingleCert());
                createStartElementAndOutputAsEvent(subOutputProcessorChain, getOffsetName(), false, null);
                createCharactersAndOutputAsEvent(subOutputProcessorChain, "" + offset);
                createEndElementAndOutputAsEvent(subOutputProcessorChain, getOffsetName());
                createStartElementAndOutputAsEvent(subOutputProcessorChain, getLengthName(), false, null);
                createCharactersAndOutputAsEvent(subOutputProcessorChain, "" + length);
                createEndElementAndOutputAsEvent(subOutputProcessorChain, getLengthName());
                createStartElementAndOutputAsEvent(subOutputProcessorChain, getNonceName(), false, null);
                createCharactersAndOutputAsEvent(subOutputProcessorChain, nonce);
                createEndElementAndOutputAsEvent(subOutputProcessorChain, getNonceName());
                createEndElementAndOutputAsEvent(subOutputProcessorChain, headerElementName);

                outputProcessorChain.removeProcessor(this);
            }
        }

        protected void createSecurityTokenReferenceStructureForDerivedKey(
                OutputProcessorChain outputProcessorChain,
                OutboundSecurityToken securityToken,
                WSSecurityTokenConstants.KeyIdentifier keyIdentifier,
                WSSConstants.DerivedKeyTokenReference derivedKeyTokenReference,
                boolean useSingleCertificate)
                throws XMLStreamException, XMLSecurityException {

            SecurityToken wrappingToken = securityToken.getKeyWrappingToken();
            List<XMLSecAttribute> attributes = new ArrayList<>(2);
            attributes.add(createAttribute(WSSConstants.ATT_WSU_ID, IDGenerator.generateID(null)));
            if (WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE.equals(keyIdentifier) && !useSingleCertificate) {
                attributes.add(createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE, WSSConstants.NS_X509_PKIPATH_V1));
            } else if (derivedKeyTokenReference == WSSConstants.DerivedKeyTokenReference.EncryptedKey
                || WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER.equals(keyIdentifier)) {
                attributes.add(createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE, WSSConstants.NS_WSS_ENC_KEY_VALUE_TYPE));
            } else if (WSSecurityTokenConstants.KERBEROS_TOKEN.equals(wrappingToken.getTokenType())) {
                attributes.add(createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE, WSSConstants.NS_GSS_KERBEROS5_AP_REQ));
            }
            createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE, false, attributes);

            X509Certificate[] x509Certificates = wrappingToken.getX509Certificates();
            String tokenId = wrappingToken.getId();

            if (derivedKeyTokenReference == WSSConstants.DerivedKeyTokenReference.EncryptedKey) {
                String valueType = WSSConstants.NS_WSS_ENC_KEY_VALUE_TYPE;
                WSSUtils.createBSTReferenceStructure(this, outputProcessorChain, tokenId, valueType, true);
            } else if (WSSecurityTokenConstants.KeyIdentifier_IssuerSerial.equals(keyIdentifier)) {
                WSSUtils.createX509IssuerSerialStructure(this, outputProcessorChain, x509Certificates);
            } else if (WSSecurityTokenConstants.KeyIdentifier_SkiKeyIdentifier.equals(keyIdentifier)) {
                WSSUtils.createX509SubjectKeyIdentifierStructure(this, outputProcessorChain, x509Certificates);
            } else if (WSSecurityTokenConstants.KeyIdentifier_X509KeyIdentifier.equals(keyIdentifier)) {
                WSSUtils.createX509KeyIdentifierStructure(this, outputProcessorChain, x509Certificates);
            } else if (WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER.equals(keyIdentifier)) {
                String identifier = wrappingToken.getSha1Identifier();
                WSSUtils.createKerberosSha1IdentifierStructure(this, outputProcessorChain, identifier);
            } else if (WSSecurityTokenConstants.KEYIDENTIFIER_THUMBPRINT_IDENTIFIER.equals(keyIdentifier)) {
                WSSUtils.createThumbprintKeyIdentifierStructure(this, outputProcessorChain, x509Certificates);
            } else if (WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE.equals(keyIdentifier)) {
                String valueType;
                if (WSSecurityTokenConstants.KERBEROS_TOKEN.equals(wrappingToken.getTokenType())) {
                    valueType = WSSConstants.NS_GSS_KERBEROS5_AP_REQ;
                } else if (WSSecurityTokenConstants.SPNEGO_CONTEXT_TOKEN.equals(wrappingToken.getTokenType())
                    || WSSecurityTokenConstants.SECURITY_CONTEXT_TOKEN.equals(wrappingToken.getTokenType())
                    || WSSecurityTokenConstants.SECURE_CONVERSATION_TOKEN.equals(wrappingToken.getTokenType())) {
                    boolean use200512Namespace = ((WSSSecurityProperties)getSecurityProperties()).isUse200512Namespace();
                    if (use200512Namespace) {
                        valueType = WSSConstants.NS_WSC_05_12 + "/sct";
                    } else {
                        valueType = WSSConstants.NS_WSC_05_02 + "/sct";
                    }
                } else if (useSingleCertificate) {
                    valueType = WSSConstants.NS_X509_V3_TYPE;
                } else {
                    valueType = WSSConstants.NS_X509_PKIPATH_V1;
                }
                boolean included = ((WSSSecurityProperties)getSecurityProperties()).isIncludeSignatureToken();
                WSSUtils.createBSTReferenceStructure(this, outputProcessorChain, tokenId, valueType, included);
            } else if (WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER.equals(keyIdentifier)) {
                WSSUtils.createEncryptedKeySha1IdentifierStructure(this, outputProcessorChain, sha1Identifier);
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "unsupportedSecurityToken");
            }
            createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE);
        }

        private QName getHeaderElementName() {
            if (use200512Namespace) {
                return WSSConstants.TAG_WSC0512_DKT;
            }
            return WSSConstants.TAG_WSC0502_DKT;
        }

        private QName getOffsetName() {
            if (use200512Namespace) {
                return WSSConstants.TAG_WSC0512_OFFSET;
            }
            return WSSConstants.TAG_WSC0502_OFFSET;
        }

        private QName getLengthName() {
            if (use200512Namespace) {
                return WSSConstants.TAG_WSC0512_LENGTH;
            }
            return WSSConstants.TAG_WSC0502_LENGTH;
        }

        private QName getNonceName() {
            if (use200512Namespace) {
                return WSSConstants.TAG_WSC0512_NONCE;
            }
            return WSSConstants.TAG_WSC0502_NONCE;
        }
    }
}
