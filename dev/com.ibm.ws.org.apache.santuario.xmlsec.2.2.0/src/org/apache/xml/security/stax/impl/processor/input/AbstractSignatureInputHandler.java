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
package org.apache.xml.security.stax.impl.processor.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.xml.bind.JAXBElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.xml.security.algorithms.implementations.SignatureBaseRSA.SignatureRSASSAPSS.DigestAlgorithm;
import org.apache.xml.security.binding.excc14n.InclusiveNamespaces;
import org.apache.xml.security.binding.xmldsig.CanonicalizationMethodType;
import org.apache.xml.security.binding.xmldsig.SignatureMethodType;
import org.apache.xml.security.binding.xmldsig.SignatureType;
import org.apache.xml.security.binding.xmldsig.SignedInfoType;
import org.apache.xml.security.binding.xmldsig.pss.RSAPSSParams;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.stax.ext.AbstractInputSecurityHeaderHandler;
import org.apache.xml.security.stax.ext.InboundSecurityContext;
import org.apache.xml.security.stax.ext.InputProcessorChain;
import org.apache.xml.security.stax.ext.Transformer;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.ext.XMLSecurityUtils;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecEventFactory;
import org.apache.xml.security.stax.impl.algorithms.SignatureAlgorithm;
import org.apache.xml.security.stax.impl.algorithms.SignatureAlgorithmFactory;
import org.apache.xml.security.stax.impl.transformer.canonicalizer.Canonicalizer20010315_Excl;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.impl.util.SignerOutputStream;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.utils.UnsyncBufferedOutputStream;
import org.apache.xml.security.utils.UnsyncByteArrayInputStream;
import org.apache.xml.security.utils.UnsyncByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.xml.security.algorithms.implementations.SignatureBaseRSA.SignatureRSASSAPSS.DigestAlgorithm.SHA256;
import static org.apache.xml.security.algorithms.implementations.SignatureBaseRSA.SignatureRSASSAPSS.DigestAlgorithm.fromXmlDigestAlgorithm;

/**
 */
public abstract class AbstractSignatureInputHandler extends AbstractInputSecurityHeaderHandler {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractSignatureInputHandler.class);
    private static final Set<String> C14N_ALGORITHMS;

    static {
        Set<String> algorithms = new HashSet<>();
        algorithms.add(XMLSecurityConstants.NS_C14N_OMIT_COMMENTS);
        algorithms.add(XMLSecurityConstants.NS_C14N_WITH_COMMENTS);
        algorithms.add(XMLSecurityConstants.NS_C14N_EXCL_OMIT_COMMENTS);
        algorithms.add(XMLSecurityConstants.NS_C14N_EXCL_WITH_COMMENTS);
        algorithms.add(XMLSecurityConstants.NS_C14N11_OMIT_COMMENTS);
        algorithms.add(XMLSecurityConstants.NS_C14N11_WITH_COMMENTS);
        C14N_ALGORITHMS = Collections.unmodifiableSet(algorithms);
    }

    @Override
    public void handle(final InputProcessorChain inputProcessorChain, final XMLSecurityProperties securityProperties,
                       Deque<XMLSecEvent> eventQueue, Integer index) throws XMLSecurityException {

        @SuppressWarnings("unchecked")
        final SignatureType signatureType = ((JAXBElement<SignatureType>) parseStructure(eventQueue, index, securityProperties)).getValue();
        if (signatureType.getSignedInfo() == null) {
            throw new XMLSecurityException("stax.signature.signedInfoMissing");
        }
        if (signatureType.getSignedInfo().getSignatureMethod() == null) {
            throw new XMLSecurityException("stax.signature.signatureMethodMissing");
        }
        if (signatureType.getSignedInfo().getCanonicalizationMethod() == null) {
            throw new XMLSecurityException("stax.signature.canonicalizationMethodMissing");
        }
        if (signatureType.getSignatureValue() == null) {
            throw new XMLSecurityException("stax.signature.signatureValueMissing");
        }
        if (signatureType.getId() == null) {
            signatureType.setId(IDGenerator.generateID(null));
        }
        InboundSecurityToken inboundSecurityToken = verifySignedInfo(inputProcessorChain, securityProperties, signatureType, eventQueue, index);
        addSignatureReferenceInputProcessorToChain(inputProcessorChain, securityProperties, signatureType, inboundSecurityToken);
    }

    protected abstract void addSignatureReferenceInputProcessorToChain(
            InputProcessorChain inputProcessorChain, XMLSecurityProperties securityProperties,
            SignatureType signatureType, InboundSecurityToken inboundSecurityToken) throws XMLSecurityException;

    protected InboundSecurityToken verifySignedInfo(InputProcessorChain inputProcessorChain, XMLSecurityProperties securityProperties,
                                             SignatureType signatureType, Deque<XMLSecEvent> eventDeque, int index)
            throws XMLSecurityException {

        Iterator<XMLSecEvent> iterator;

        String c14NMethod = signatureType.getSignedInfo().getCanonicalizationMethod().getAlgorithm();
        if (c14NMethod != null && C14N_ALGORITHMS.contains(c14NMethod)) {

            iterator = eventDeque.descendingIterator();
            //forward to <Signature> Element
            int i = 0;
            while (i < index) {
                iterator.next();
                i++;
            }

        } else {
            iterator = reparseSignedInfo(inputProcessorChain, securityProperties, signatureType, eventDeque, index).descendingIterator();
            index = 0;
        }

        SignatureVerifier signatureVerifier = newSignatureVerifier(inputProcessorChain, securityProperties, signatureType);

        try {
            loop:
            while (iterator.hasNext()) {
                XMLSecEvent xmlSecEvent = iterator.next();
                if (XMLStreamConstants.START_ELEMENT == xmlSecEvent.getEventType()
                    && xmlSecEvent.asStartElement().getName().equals(XMLSecurityConstants.TAG_dsig_SignedInfo)) {
                    signatureVerifier.processEvent(xmlSecEvent);
                    break loop;
                }
            }
            loop:
            while (iterator.hasNext()) {
                XMLSecEvent xmlSecEvent = iterator.next();
                signatureVerifier.processEvent(xmlSecEvent);
                if (XMLStreamConstants.END_ELEMENT == xmlSecEvent.getEventType()
                    && xmlSecEvent.asEndElement().getName().equals(XMLSecurityConstants.TAG_dsig_SignedInfo)) {
                    break loop;
                }
            }
        } catch (XMLStreamException e) {
            throw new XMLSecurityException(e);
        }
        signatureVerifier.doFinal();
        return signatureVerifier.getInboundSecurityToken();
    }

    protected Deque<XMLSecEvent> reparseSignedInfo(InputProcessorChain inputProcessorChain, XMLSecurityProperties securityProperties,
                                                   SignatureType signatureType, Deque<XMLSecEvent> eventDeque, int index
    ) throws XMLSecurityException {

        Deque<XMLSecEvent> signedInfoDeque = new ArrayDeque<>();

        try (UnsyncByteArrayOutputStream unsynchronizedByteArrayOutputStream = new UnsyncByteArrayOutputStream()) {
            Transformer transformer = XMLSecurityUtils.getTransformer(
                    null,
                    unsynchronizedByteArrayOutputStream,
                    null,
                    signatureType.getSignedInfo().getCanonicalizationMethod().getAlgorithm(),
                    XMLSecurityConstants.DIRECTION.IN);

            Iterator<XMLSecEvent> iterator = eventDeque.descendingIterator();
            //forward to <Signature> Element
            int i = 0;
            while (i < index) {
                iterator.next();
                i++;
            }

            loop:
            while (iterator.hasNext()) {
                XMLSecEvent xmlSecEvent = iterator.next();
                if (XMLStreamConstants.START_ELEMENT == xmlSecEvent.getEventType()
                    && xmlSecEvent.asStartElement().getName().equals(XMLSecurityConstants.TAG_dsig_SignedInfo)) {
                    transformer.transform(xmlSecEvent);
                    break loop;
                }
            }

            loop:
            while (iterator.hasNext()) {
                XMLSecEvent xmlSecEvent = iterator.next();
                transformer.transform(xmlSecEvent);
                if (XMLStreamConstants.END_ELEMENT == xmlSecEvent.getEventType()
                    && xmlSecEvent.asEndElement().getName().equals(XMLSecurityConstants.TAG_dsig_SignedInfo)) {
                    break loop;
                }
            }

            transformer.doFinal();

            try (InputStream is = new UnsyncByteArrayInputStream(unsynchronizedByteArrayOutputStream.toByteArray())) {
                XMLStreamReader xmlStreamReader = inputProcessorChain.getSecurityContext().
                        <XMLInputFactory>get(XMLSecurityConstants.XMLINPUTFACTORY).
                        createXMLStreamReader(is);

                while (xmlStreamReader.hasNext()) {
                    XMLSecEvent xmlSecEvent = XMLSecEventFactory.allocate(xmlStreamReader, null);
                    signedInfoDeque.push(xmlSecEvent);
                    xmlStreamReader.next();
                }

                @SuppressWarnings("unchecked")
                final SignedInfoType signedInfoType =
                        ((JAXBElement<SignedInfoType>) parseStructure(signedInfoDeque, 0, securityProperties)).getValue();
                signatureType.setSignedInfo(signedInfoType);

                return signedInfoDeque;
            }
        } catch (XMLStreamException | IOException e) {
            throw new XMLSecurityException(e);
        }
    }

    protected abstract SignatureVerifier newSignatureVerifier(InputProcessorChain inputProcessorChain,
                                                              XMLSecurityProperties securityProperties,
                                                              final SignatureType signatureType) throws XMLSecurityException;

/*
    <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#" Id="Signature-1022834285">
        <ds:SignedInfo>
            <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#" />
            <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1" />
            <ds:Reference URI="#id-1612925417">
                <ds:Transforms>
                    <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#" />
                </ds:Transforms>
                <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1" />
                <ds:DigestValue>cy/khx5N6UobCJ1EbX+qnrGID2U=</ds:DigestValue>
            </ds:Reference>
            <ds:Reference URI="#Timestamp-1106985890">
                <ds:Transforms>
                    <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#" />
                </ds:Transforms>
                <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1" />
                <ds:DigestValue>+p5YRII6uvUdsJ7XLKkWx1CBewE=</ds:DigestValue>
            </ds:Reference>
        </ds:SignedInfo>
        <ds:SignatureValue>
            Izg1FlI9oa4gOon2vTXi7V0EpiyCUazECVGYflbXq7/3GF8ThKGDMpush/fo1I2NVjEFTfmT2WP/
            +ZG5N2jASFptrcGbsqmuLE5JbxUP1TVKb9SigKYcOQJJ8klzmVfPXnSiRZmIU+DUT2UXopWnGNFL
            TwY0Uxja4ZuI6U8m8Tg=
        </ds:SignatureValue>
        <ds:KeyInfo Id="KeyId-1043455692">
            <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="STRId-1008354042">
                <wsse:Reference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                URI="#CertId-3458500" ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3" />
            </wsse:SecurityTokenReference>
        </ds:KeyInfo>
    </ds:Signature>
     */

    public abstract class SignatureVerifier {

        private final SignatureType signatureType;
        private final InboundSecurityToken inboundSecurityToken;

        private SignerOutputStream signerOutputStream;
        private OutputStream bufferedSignerOutputStream;
        private Transformer transformer;

        public SignatureVerifier(SignatureType signatureType, InboundSecurityContext inboundSecurityContext,
                                 XMLSecurityProperties securityProperties) throws XMLSecurityException {
            this.signatureType = signatureType;

            InboundSecurityToken inboundSecurityToken =
                retrieveSecurityToken(signatureType, securityProperties, inboundSecurityContext);
            this.inboundSecurityToken = inboundSecurityToken;

            createSignatureAlgorithm(inboundSecurityToken, signatureType);
        }

        protected abstract InboundSecurityToken retrieveSecurityToken(SignatureType signatureType,
                                                 XMLSecurityProperties securityProperties,
                                                 InboundSecurityContext inboundSecurityContext) throws XMLSecurityException;

        public InboundSecurityToken getInboundSecurityToken() {
            return inboundSecurityToken;
        }

        protected void createSignatureAlgorithm(InboundSecurityToken inboundSecurityToken, SignatureType signatureType)
                throws XMLSecurityException {

            Key verifyKey;
            final String algorithmURI = signatureType.getSignedInfo().getSignatureMethod().getAlgorithm();
            if (inboundSecurityToken.isAsymmetric()) {
                verifyKey = inboundSecurityToken.getPublicKey(algorithmURI, XMLSecurityConstants.Asym_Sig, signatureType.getId());
            } else {
                verifyKey = inboundSecurityToken.getSecretKey(
                        algorithmURI, XMLSecurityConstants.Sym_Sig, signatureType.getId());
                if (verifyKey != null) {
                    verifyKey = XMLSecurityUtils.prepareSecretKey(algorithmURI, verifyKey.getEncoded());
                }
            }

            if (verifyKey == null) {
                throw new XMLSecurityException("KeyInfo.nokey", new Object[]{"the inbound security token"});
            }

            try {
                SignatureAlgorithm signatureAlgorithm =
                        SignatureAlgorithmFactory.getInstance().getSignatureAlgorithm(
                                algorithmURI);
                if (XMLSignature.ALGO_ID_SIGNATURE_RSA_PSS.equals(algorithmURI)) {
                    PSSParameterSpec spec = rsaPSSParameterSpec(signatureType);
                    signatureAlgorithm.engineSetParameter(spec);
                }
                signatureAlgorithm.engineInitVerify(verifyKey);
                signerOutputStream = new SignerOutputStream(signatureAlgorithm);
                bufferedSignerOutputStream = new UnsyncBufferedOutputStream(signerOutputStream);

                final CanonicalizationMethodType canonicalizationMethodType =
                        signatureType.getSignedInfo().getCanonicalizationMethod();
                InclusiveNamespaces inclusiveNamespacesType =
                        XMLSecurityUtils.getQNameType(
                                canonicalizationMethodType.getContent(),
                                XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces
                        );

                Map<String, Object> transformerProperties = null;
                if (inclusiveNamespacesType != null) {
                    transformerProperties = new HashMap<>();
                    transformerProperties.put(
                            Canonicalizer20010315_Excl.INCLUSIVE_NAMESPACES_PREFIX_LIST,
                            inclusiveNamespacesType.getPrefixList());
                }
                transformer = XMLSecurityUtils.getTransformer(
                        null,
                        this.bufferedSignerOutputStream,
                        transformerProperties,
                        canonicalizationMethodType.getAlgorithm(),
                        XMLSecurityConstants.DIRECTION.IN);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new XMLSecurityException(e);
            }

            // Clean the secret key from memory now that we're done with it
            if (verifyKey instanceof Destroyable) {
                try {
                    ((Destroyable)verifyKey).destroy();
                } catch (DestroyFailedException e) {
                    LOG.debug("Error destroying key: {}", e.getMessage());
                }
            }
        }

        private PSSParameterSpec rsaPSSParameterSpec(SignatureType signatureType) throws XMLSecurityException {
            SignatureMethodType signatureMethod = signatureType.getSignedInfo().getSignatureMethod();
            RSAPSSParams rsapssParams = null;
            for (Object o : signatureMethod.getContent()) {
                if (o instanceof RSAPSSParams) {
                    rsapssParams = (RSAPSSParams) o;
                    break;
                }
            }
            if (rsapssParams == null) {
                throw new XMLSecurityException("algorithms.MissingRSAPSSParams");
            }

            String digestMethod = rsapssParams.getDigestMethod() == null ? SHA256.getXmlDigestAlgorithm() : rsapssParams.getDigestMethod().getAlgorithm();
            String maskGenerationDigestMethod = rsapssParams.getMaskGenerationFunction() == null ? SHA256.getXmlDigestAlgorithm() : rsapssParams.getMaskGenerationFunction().getDigestMethod().getAlgorithm();
            DigestAlgorithm digestAlgorithm = fromXmlDigestAlgorithm(digestMethod);

            int saltLength = rsapssParams.getSaltLength() == null ? digestAlgorithm.getSaltLength() : rsapssParams.getSaltLength();
            int trailerField = rsapssParams.getTrailerField() == null ? 1 : rsapssParams.getTrailerField();
            String maskDigestAlgorithm = fromXmlDigestAlgorithm(maskGenerationDigestMethod).getDigestAlgorithm();

            return new PSSParameterSpec(digestAlgorithm.getDigestAlgorithm(), "MGF1",
                                        new MGF1ParameterSpec(maskDigestAlgorithm), saltLength, trailerField);
        }

        protected void processEvent(XMLSecEvent xmlSecEvent) throws XMLStreamException {
            transformer.transform(xmlSecEvent);
        }

        protected void doFinal() throws XMLSecurityException {
            try {
                transformer.doFinal();
                bufferedSignerOutputStream.close();
            } catch (IOException | XMLStreamException e) {
                throw new XMLSecurityException(e);
            }
            if (!signerOutputStream.verify(signatureType.getSignatureValue().getValue())) {
                throw new XMLSecurityException("errorMessages.InvalidSignatureValueException");
            }
        }
    }
}
