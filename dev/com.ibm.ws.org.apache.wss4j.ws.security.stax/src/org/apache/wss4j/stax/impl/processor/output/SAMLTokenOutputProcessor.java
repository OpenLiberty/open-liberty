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

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.stax.ext.WSSConfigurationException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurePart;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.config.JCEAlgorithmMapper;
import org.apache.xml.security.stax.ext.AbstractOutputProcessor;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.impl.securityToken.GenericOutboundSecurityToken;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;
import org.opensaml.saml.common.SAMLVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SAMLTokenOutputProcessor extends AbstractOutputProcessor {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SAMLTokenOutputProcessor.class);

    public SAMLTokenOutputProcessor() throws XMLSecurityException {
        super();
        addBeforeProcessor(BinarySecurityTokenOutputProcessor.class);
        addBeforeProcessor(WSSSignatureOutputProcessor.class);
    }

    @Override
    public void processEvent(XMLSecEvent xmlSecEvent, final OutputProcessorChain outputProcessorChain)
            throws XMLStreamException, XMLSecurityException {

        try {
            final SAMLCallback samlCallback = new SAMLCallback();
            SAMLUtil.doSAMLCallback(((WSSSecurityProperties) getSecurityProperties()).getSamlCallbackHandler(), samlCallback);
            SamlAssertionWrapper samlAssertionWrapper = new SamlAssertionWrapper(samlCallback);

            if (samlCallback.isSignAssertion()) {
                samlAssertionWrapper.signAssertion(
                        samlCallback.getIssuerKeyName(),
                        samlCallback.getIssuerKeyPassword(),
                        samlCallback.getIssuerCrypto(),
                        samlCallback.isSendKeyValue(),
                        samlCallback.getCanonicalizationAlgorithm(),
                        samlCallback.getSignatureAlgorithm(),
                        samlCallback.getSignatureDigestAlgorithm()
                );
            }

            boolean senderVouches = false;
            boolean hok = false;
            List<String> methods = samlAssertionWrapper.getConfirmationMethods();
            if (methods != null && !methods.isEmpty()) {
                String confirmMethod = methods.get(0);
                if (OpenSAMLUtil.isMethodSenderVouches(confirmMethod)) {
                    senderVouches = true;
                } else if (OpenSAMLUtil.isMethodHolderOfKey(confirmMethod)) {
                    hok = true;
                }
            }

            final String securityTokenReferenceId = IDGenerator.generateID(null);
            final String tokenId = samlAssertionWrapper.getId();

            final FinalSAMLTokenOutputProcessor finalSAMLTokenOutputProcessor;

            XMLSecurityConstants.Action action = getAction();
            boolean includeSTR = false;

            GenericOutboundSecurityToken securityToken = null;

            // See if a token is already available
            String sigTokenId =
                outputProcessorChain.getSecurityContext().get(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE);
            SecurityTokenProvider<OutboundSecurityToken> signatureTokenProvider = null;
            if (sigTokenId != null) {
                signatureTokenProvider =
                    outputProcessorChain.getSecurityContext().getSecurityTokenProvider(sigTokenId);
                if (signatureTokenProvider != null) {
                    securityToken =
                        (GenericOutboundSecurityToken)signatureTokenProvider.getSecurityToken();
                }
            }

            if (WSSConstants.SAML_TOKEN_SIGNED.equals(action) && senderVouches) {
                includeSTR = true;
                if (securityToken == null) {
                    securityToken = getSecurityToken(samlCallback, outputProcessorChain);
                }

                finalSAMLTokenOutputProcessor = new FinalSAMLTokenOutputProcessor(securityToken, samlAssertionWrapper,
                        securityTokenReferenceId, senderVouches, includeSTR);
                finalSAMLTokenOutputProcessor.setAction(getAction(), getActionOrder());

                securityToken.setProcessor(finalSAMLTokenOutputProcessor);

            } else if (WSSConstants.SAML_TOKEN_SIGNED.equals(action) && hok) {

                final Element ref;
                if (securityToken != null) {
                    ref = securityToken.getCustomTokenReference();
                } else {
                    ref = null;
                }

                finalSAMLTokenOutputProcessor = new FinalSAMLTokenOutputProcessor(null, samlAssertionWrapper,
                        securityTokenReferenceId, senderVouches, includeSTR);

                final SAMLSecurityTokenProvider securityTokenProvider =
                    new SAMLSecurityTokenProvider(samlCallback, (WSSSecurityProperties)getSecurityProperties(),
                                                  tokenId, ref, finalSAMLTokenOutputProcessor);

                //fire a tokenSecurityEvent
                TokenSecurityEvent<OutboundSecurityToken> tokenSecurityEvent =
                    new TokenSecurityEvent<OutboundSecurityToken>(WSSecurityEventConstants.SAML_TOKEN) {

                    public OutboundSecurityToken getSecurityToken() {
                        try {
                            return securityTokenProvider.getSecurityToken();
                        } catch (XMLSecurityException e) {
                            LOG.debug(e.getMessage(), e);
                        }
                        return null;
                    }
                };
                outputProcessorChain.getSecurityContext().registerSecurityEvent(tokenSecurityEvent);

                outputProcessorChain.getSecurityContext().registerSecurityTokenProvider(tokenId, securityTokenProvider);
                outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, tokenId);
            } else if (WSSConstants.SAML_TOKEN_UNSIGNED.equals(getAction())) {
                // Check to see whether this token is to be signed by the message signature. If so,
                // output a STR to be signed instead, and remove this Assertion from the signature parts
                // list
                QName assertionName = new QName(WSSConstants.NS_SAML2, "Assertion");
                if (samlAssertionWrapper.getSamlVersion() == SAMLVersion.VERSION_11) {
                    assertionName = new QName(WSSConstants.NS_SAML, "Assertion");
                }

                Iterator<SecurePart> signaturePartsIterator =
                    securityProperties.getSignatureSecureParts().iterator();
                while (signaturePartsIterator.hasNext()) {
                    SecurePart securePart = signaturePartsIterator.next();
                    if (samlAssertionWrapper.getId().equals(securePart.getIdToSecure())
                        || assertionName.equals(securePart.getName())) {
                        includeSTR = true;
                        signaturePartsIterator.remove();
                        break;
                    }
                }

                finalSAMLTokenOutputProcessor = new FinalSAMLTokenOutputProcessor(null, samlAssertionWrapper,
                                                                                  securityTokenReferenceId, senderVouches,
                                                                                  includeSTR);
                if (includeSTR) {
                    finalSAMLTokenOutputProcessor.addBeforeProcessor(WSSSignatureOutputProcessor.class);
                }
            } else {
                finalSAMLTokenOutputProcessor = new FinalSAMLTokenOutputProcessor(null, samlAssertionWrapper,
                                                                                  securityTokenReferenceId, senderVouches,
                                                                                  includeSTR);
            }

            finalSAMLTokenOutputProcessor.setXMLSecurityProperties(getSecurityProperties());
            finalSAMLTokenOutputProcessor.setAction(action, getActionOrder());
            finalSAMLTokenOutputProcessor.init(outputProcessorChain);

            if (includeSTR) {
                WSSSecurePart securePart =
                        new WSSSecurePart(
                                new QName(WSSConstants.SOAPMESSAGE_NS10_STR_TRANSFORM), SecurePart.Modifier.Element);
                securePart.setIdToSecure(tokenId);
                securePart.setIdToReference(securityTokenReferenceId);
                outputProcessorChain.getSecurityContext().putAsMap(WSSConstants.SIGNATURE_PARTS, tokenId, securePart);
            }

        } finally {
            outputProcessorChain.removeProcessor(this);
        }
        outputProcessorChain.processEvent(xmlSecEvent);
    }

    private GenericOutboundSecurityToken getSecurityToken(SAMLCallback samlCallback,
                                              OutputProcessorChain outputProcessorChain) throws WSSecurityException {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(samlCallback.getIssuerKeyName());
        X509Certificate[] certificates = null;
        if (samlCallback.getIssuerCrypto() != null) {
            certificates = samlCallback.getIssuerCrypto().getX509Certificates(cryptoType);
        }
        if (certificates == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                    "empty",
                    new Object[] {"No issuer certs were found to sign the SAML Assertion using issuer name: "
                    + samlCallback.getIssuerKeyName()}
            );
        }

        PrivateKey privateKey;
        try {
            privateKey = samlCallback.getIssuerCrypto().getPrivateKey(
                    samlCallback.getIssuerKeyName(), samlCallback.getIssuerKeyPassword());
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }

        final String binarySecurityTokenId = IDGenerator.generateID(null);

        final GenericOutboundSecurityToken bstSecurityToken =
                new GenericOutboundSecurityToken(binarySecurityTokenId, WSSecurityTokenConstants.X509V3Token,
                        privateKey, certificates);

        SecurityTokenProvider<OutboundSecurityToken> securityTokenProvider =
            new SecurityTokenProvider<OutboundSecurityToken>() {

            @Override
            public OutboundSecurityToken getSecurityToken() throws WSSecurityException {
                return bstSecurityToken;
            }

            @Override
            public String getId() {
                return binarySecurityTokenId;
            }
        };

        outputProcessorChain.getSecurityContext().registerSecurityTokenProvider(binarySecurityTokenId,
                                                                                securityTokenProvider);
        outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE,
                                                      binarySecurityTokenId);

        return bstSecurityToken;
    }

    private static class SAMLSecurityTokenProvider
        implements SecurityTokenProvider<OutboundSecurityToken> {

        private GenericOutboundSecurityToken samlSecurityToken;
        private SAMLCallback samlCallback;
        private String tokenId;
        private Element ref;
        private FinalSAMLTokenOutputProcessor finalSAMLTokenOutputProcessor;
        private WSSSecurityProperties securityProperties;

        SAMLSecurityTokenProvider(SAMLCallback samlCallback, WSSSecurityProperties securityProperties, String tokenId,
                                         Element ref, FinalSAMLTokenOutputProcessor finalSAMLTokenOutputProcessor) {
            this.samlCallback = samlCallback;
            this.securityProperties = securityProperties;
            this.tokenId = tokenId;
            this.ref = ref;
            this.finalSAMLTokenOutputProcessor = finalSAMLTokenOutputProcessor;
        }

        @Override
        public OutboundSecurityToken getSecurityToken() throws XMLSecurityException {

            if (this.samlSecurityToken != null) {
                return this.samlSecurityToken;
            }

            WSSecurityTokenConstants.TokenType tokenType;
            if (samlCallback.getSamlVersion() == SAMLVersion.VERSION_10) {
                tokenType = WSSecurityTokenConstants.SAML_10_TOKEN;
            } else if (samlCallback.getSamlVersion() == SAMLVersion.VERSION_11) {
                tokenType = WSSecurityTokenConstants.SAML_11_TOKEN;
            } else {
                tokenType = WSSecurityTokenConstants.SAML_20_TOKEN;
            }

            PrivateKey privateKey = getPrivateKeyUsingCallback();
            if (privateKey != null) {
                this.samlSecurityToken = new GenericOutboundSecurityToken(
                        tokenId, tokenType, privateKey, getCertificatesUsingCallback());
            } else {
                this.samlSecurityToken = new GenericOutboundSecurityToken(
                        tokenId, tokenType) {

                    @Override
                    public Key getSecretKey(String algorithmURI) throws WSSecurityException {

                        Key key;
                        try {
                            key = super.getSecretKey(algorithmURI);
                        } catch (XMLSecurityException e) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
                        }
                        if (key != null) {
                            return key;
                        }
                        byte[] secretKey = getSecretKeyUsingCallback();
                        if (secretKey != null && secretKey.length > 0) {
                            String algoFamily = JCEAlgorithmMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
                            key = new SecretKeySpec(secretKey, algoFamily);
                            setSecretKey(algorithmURI, key);
                        }
                        return key;
                    }
                };
            }
            this.samlSecurityToken.setProcessor(finalSAMLTokenOutputProcessor);
            this.samlSecurityToken.setCustomTokenReference(ref);
            return this.samlSecurityToken;
        }

        private PrivateKey getPrivateKeyUsingCallback()
            throws WSSConfigurationException, WSSecurityException {

            SubjectBean subjectBean = samlCallback.getSubject();
            if (subjectBean != null) {
                KeyInfoBean keyInfoBean = subjectBean.getKeyInfo();
                if (keyInfoBean != null) {
                    X509Certificate x509Certificate = keyInfoBean.getCertificate();
                    if (x509Certificate != null) {
                        String alias = securityProperties.getSignatureCrypto().getX509Identifier(x509Certificate);
                        if (alias == null) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "aliasIsNull");
                        }
                        WSPasswordCallback wsPasswordCallback =
                            new WSPasswordCallback(alias, WSPasswordCallback.SIGNATURE);
                        WSSUtils.doPasswordCallback(securityProperties.getCallbackHandler(), wsPasswordCallback);
                        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                        cryptoType.setAlias(alias);
                        return securityProperties.getSignatureCrypto().getPrivateKey(alias, wsPasswordCallback.getPassword());
                    } else if (keyInfoBean.getPublicKey() != null) {
                        return securityProperties.getSignatureCrypto().getPrivateKey(
                                        samlCallback.getIssuerKeyName(), samlCallback.getIssuerKeyPassword());
                    }
                }
            }

            return null;
        }

        private X509Certificate[] getCertificatesUsingCallback()
            throws WSSConfigurationException, WSSecurityException {

            SubjectBean subjectBean = samlCallback.getSubject();
            if (subjectBean != null) {
                KeyInfoBean keyInfoBean = subjectBean.getKeyInfo();
                if (keyInfoBean != null) {
                    X509Certificate x509Certificate = keyInfoBean.getCertificate();
                    if (x509Certificate != null) {
                        String alias = securityProperties.getSignatureCrypto().getX509Identifier(x509Certificate);
                        if (alias == null) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "aliasIsNull");
                        }
                        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                        cryptoType.setAlias(alias);
                        return securityProperties.getSignatureCrypto().getX509Certificates(cryptoType);
                    }
                }
            }

            return new X509Certificate[0];
        }


        private byte[] getSecretKeyUsingCallback()
            throws WSSConfigurationException, WSSecurityException {

            SubjectBean subjectBean = samlCallback.getSubject();
            if (subjectBean != null) {
                KeyInfoBean keyInfoBean = subjectBean.getKeyInfo();
                if (keyInfoBean != null && keyInfoBean.getCertificate() == null && keyInfoBean.getPublicKey() == null) {
                   return keyInfoBean.getEphemeralKey();
                }
            }

            return new byte[0];
        }

        @Override
        public String getId() {
            return tokenId;
        }
    }

    class FinalSAMLTokenOutputProcessor extends AbstractOutputProcessor {

        private final OutboundSecurityToken securityToken;
        private final SamlAssertionWrapper samlAssertionWrapper;
        private final String securityTokenReferenceId;
        private boolean senderVouches = false;
        private boolean includeSTR = false;

        FinalSAMLTokenOutputProcessor(OutboundSecurityToken securityToken, SamlAssertionWrapper samlAssertionWrapper,
                                      String securityTokenReferenceId, boolean senderVouches,
                                      boolean includeSTR) throws XMLSecurityException {
            super();
            this.addAfterProcessor(UsernameTokenOutputProcessor.class);
            this.addAfterProcessor(SAMLTokenOutputProcessor.class);
            this.addBeforeProcessor(WSSSignatureOutputProcessor.class);
            this.samlAssertionWrapper = samlAssertionWrapper;
            this.securityTokenReferenceId = securityTokenReferenceId;
            this.senderVouches = senderVouches;
            this.securityToken = securityToken;
            this.includeSTR = includeSTR;
        }

        @Override
        public void processEvent(XMLSecEvent xmlSecEvent, OutputProcessorChain outputProcessorChain)
                throws XMLStreamException, XMLSecurityException {

            outputProcessorChain.processEvent(xmlSecEvent);

            if (WSSUtils.isSecurityHeaderElement(xmlSecEvent, ((WSSSecurityProperties) getSecurityProperties()).getActor())) {

                OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);
                if (includeBST()) {

                    OutputProcessorUtils.updateSecurityHeaderOrder(
                            outputProcessorChain, WSSConstants.TAG_WSSE_BINARY_SECURITY_TOKEN, getAction(), false);

                    WSSUtils.createBinarySecurityTokenStructure(this, outputProcessorChain, securityToken.getId(),
                            securityToken.getX509Certificates(), getSecurityProperties().isUseSingleCert());
                }

                final QName headerElementName;
                if (samlAssertionWrapper.getSamlVersion() == SAMLVersion.VERSION_11) {
                    headerElementName = WSSConstants.TAG_SAML_ASSERTION;
                } else {
                    headerElementName = WSSConstants.TAG_SAML2_ASSERTION;
                }
                OutputProcessorUtils.updateSecurityHeaderOrder(outputProcessorChain, headerElementName, getAction(), false);

                try {
                    Document doc = ((WSSSecurityProperties) getSecurityProperties()).getDocumentCreator().newDocument();
                    outputDOMElement(samlAssertionWrapper.toDOM(doc), subOutputProcessorChain);
                } catch (ParserConfigurationException ex) {
                    LOG.debug("Error writing out SAML Assertion", ex);
                    throw new XMLSecurityException(ex);
                }
                if (includeSTR) {
                    OutputProcessorUtils.updateSecurityHeaderOrder(
                            outputProcessorChain, WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE, getAction(), false);
                    outputSecurityTokenReference(subOutputProcessorChain, samlAssertionWrapper,
                            securityTokenReferenceId, samlAssertionWrapper.getId());
                }
                outputProcessorChain.removeProcessor(this);
            }
        }

        private boolean includeBST() {
            return senderVouches
                && getSecurityProperties().getSignatureKeyIdentifiers().contains(
                    WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE)
                && securityToken != null
                && !(WSSConstants.SAML_TOKEN_SIGNED.equals(action)
                    && ((WSSSecurityProperties)getSecurityProperties()).isIncludeSignatureToken());
        }
    }

    private void outputSecurityTokenReference(
            OutputProcessorChain outputProcessorChain, SamlAssertionWrapper samlAssertionWrapper,
            String referenceId, String tokenId) throws XMLStreamException, XMLSecurityException {

        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        WSSecurityTokenConstants.TokenType tokenType = WSSecurityTokenConstants.SAML_11_TOKEN;
        if (samlAssertionWrapper.getSamlVersion() == SAMLVersion.VERSION_11) {
            attributes.add(createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE, WSSConstants.NS_SAML11_TOKEN_PROFILE_TYPE));
        } else {
            tokenType = WSSecurityTokenConstants.SAML_20_TOKEN;
            attributes.add(createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE, WSSConstants.NS_SAML20_TOKEN_PROFILE_TYPE));
        }
        attributes.add(createAttribute(WSSConstants.ATT_WSU_ID, referenceId));
        createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE, false, attributes);
        WSSUtils.createSAMLKeyIdentifierStructure(this, outputProcessorChain, tokenType, tokenId);
        createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE);
    }

}
