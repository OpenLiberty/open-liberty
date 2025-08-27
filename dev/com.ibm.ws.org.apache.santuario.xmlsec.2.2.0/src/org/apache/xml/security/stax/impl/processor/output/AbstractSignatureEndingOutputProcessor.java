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
package org.apache.xml.security.stax.impl.processor.output;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.xml.stream.XMLStreamException;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.AbstractBufferingOutputProcessor;
import org.apache.xml.security.stax.ext.AbstractOutputProcessor;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.Transformer;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityUtils;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.SignaturePartDef;
import org.apache.xml.security.stax.impl.algorithms.SignatureAlgorithm;
import org.apache.xml.security.stax.impl.algorithms.SignatureAlgorithmFactory;
import org.apache.xml.security.stax.impl.transformer.canonicalizer.Canonicalizer20010315_Excl;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.impl.util.SignerOutputStream;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;
import org.apache.xml.security.utils.UnsyncBufferedOutputStream;
import org.apache.xml.security.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.xml.security.algorithms.implementations.SignatureBaseRSA.SignatureRSASSAPSS.DigestAlgorithm.fromDigestAlgorithm;

/**
 */
public abstract class AbstractSignatureEndingOutputProcessor extends AbstractBufferingOutputProcessor {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractSignatureEndingOutputProcessor.class);

    private List<SignaturePartDef> signaturePartDefList;

    public AbstractSignatureEndingOutputProcessor(AbstractSignatureOutputProcessor signatureOutputProcessor)
            throws XMLSecurityException {
        super();
        signaturePartDefList = signatureOutputProcessor.getSignaturePartDefList();
    }

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

    @Override
    public void processHeaderEvent(OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {

        OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);

        List<XMLSecAttribute> attributes = new ArrayList<>(1);
        String signatureId = null;
        if (securityProperties.isSignatureGenerateIds()) {
            attributes = new ArrayList<>(1);
            signatureId = IDGenerator.generateID(null);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Id, signatureId));
        } else {
            attributes = Collections.emptyList();
        }

        XMLSecStartElement signatureElement = createStartElementAndOutputAsEvent(subOutputProcessorChain,
                XMLSecurityConstants.TAG_dsig_Signature, true, attributes);

        SignatureAlgorithm signatureAlgorithm;
        try {
            signatureAlgorithm = SignatureAlgorithmFactory.getInstance().getSignatureAlgorithm(
                    getSecurityProperties().getSignatureAlgorithm());
            if (getSecurityProperties().getAlgorithmParameterSpec() != null) {
                signatureAlgorithm.engineSetParameter(getSecurityProperties().getAlgorithmParameterSpec());
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new XMLSecurityException(e);
        }

        String tokenId = outputProcessorChain.getSecurityContext().get(XMLSecurityConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE);
        if (tokenId == null) {
            throw new XMLSecurityException("stax.keyNotFound");
        }
        SecurityTokenProvider<OutboundSecurityToken> wrappingSecurityTokenProvider =
                outputProcessorChain.getSecurityContext().getSecurityTokenProvider(tokenId);
        if (wrappingSecurityTokenProvider == null) {
            throw new XMLSecurityException("stax.keyNotFound");
        }
        final OutboundSecurityToken wrappingSecurityToken = wrappingSecurityTokenProvider.getSecurityToken();
        if (wrappingSecurityToken == null) {
            throw new XMLSecurityException("stax.keyNotFound");
        }

        String sigAlgorithm = getSecurityProperties().getSignatureAlgorithm();
        Key key = wrappingSecurityToken.getSecretKey(sigAlgorithm);
        //todo remove and use wrappingSecurityToken.isSymmetric or so?
        if (XMLSecurityConstants.NS_XMLDSIG_HMACSHA1.equals(sigAlgorithm)) {
            key = XMLSecurityUtils.prepareSecretKey(sigAlgorithm, key.getEncoded());
        }
        signatureAlgorithm.engineInitSign(key);

        SignedInfoProcessor signedInfoProcessor =
            newSignedInfoProcessor(signatureAlgorithm, signatureId, signatureElement, subOutputProcessorChain);
        createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignedInfo, false, null);

        attributes = new ArrayList<>(1);
        final String signatureCanonicalizationAlgorithm = getSecurityProperties().getSignatureCanonicalizationAlgorithm();
        attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm, signatureCanonicalizationAlgorithm));
        createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_CanonicalizationMethod, false, attributes);

        if (getSecurityProperties().isAddExcC14NInclusivePrefixes() && XMLSecurityConstants.NS_C14N_EXCL.equals(signatureCanonicalizationAlgorithm)) {
            attributes = new ArrayList<>(1);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_PrefixList, signedInfoProcessor.getInclusiveNamespacePrefixes()));
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces, true, attributes);
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces);
        }

        createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_CanonicalizationMethod);

        attributes = new ArrayList<>(1);
        attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm, getSecurityProperties().getSignatureAlgorithm()));
        createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignatureMethod, false, attributes);

        if (getSecurityProperties().getAlgorithmParameterSpec() instanceof PSSParameterSpec) {
            PSSParameterSpec pssParams = (PSSParameterSpec) getSecurityProperties().getAlgorithmParameterSpec();
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_RSAPSSPARAMS, false, null);

            attributes = new ArrayList<>(1);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm, fromDigestAlgorithm(pssParams.getDigestAlgorithm()).getXmlDigestAlgorithm()));
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestMethod, false, attributes);
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestMethod);

            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_SALTLENGTH, false, null);
            createCharactersAndOutputAsEvent(subOutputProcessorChain, String.valueOf(pssParams.getSaltLength()));
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_SALTLENGTH);

            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_TRAILERFIELD, false, null);
            createCharactersAndOutputAsEvent(subOutputProcessorChain, String.valueOf(pssParams.getTrailerField()));
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_TRAILERFIELD);

            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsigmore_RSAPSSPARAMS);
        }

        createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignatureMethod);

        for (SignaturePartDef signaturePartDef : signaturePartDefList) {
            String uriString;
            if (signaturePartDef.isExternalResource()) {
                uriString = signaturePartDef.getSigRefId();
            } else if (signaturePartDef.getSigRefId() != null) {
                if (signaturePartDef.isGenerateXPointer()) {
                    uriString = "#xpointer(id('" + signaturePartDef.getSigRefId() + "'))";
                } else {
                    uriString = "#" + signaturePartDef.getSigRefId();
                }
            } else {
                uriString = "";
            }
            attributes = new ArrayList<>(1);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_URI, uriString));
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Reference, false, attributes);
            createTransformsStructureForSignature(subOutputProcessorChain, signaturePartDef);

            attributes = new ArrayList<>(1);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm, signaturePartDef.getDigestAlgo()));
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestMethod, false, attributes);
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestMethod);
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestValue, false, null);
            createCharactersAndOutputAsEvent(subOutputProcessorChain, signaturePartDef.getDigestValue());
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_DigestValue);
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Reference);
        }

        createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignedInfo);
        subOutputProcessorChain.removeProcessor(signedInfoProcessor);

        createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignatureValue, false, null);
        final byte[] signatureValue = signedInfoProcessor.getSignatureValue();
        createCharactersAndOutputAsEvent(subOutputProcessorChain, XMLUtils.encodeToString(signatureValue));
        createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_SignatureValue);

        if (securityProperties.isSignatureGenerateIds()) {
            attributes = new ArrayList<>(1);
            attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Id, IDGenerator.generateID(null)));
        } else {
            attributes = Collections.emptyList();
        }

        if (!getSecurityProperties().getSignatureKeyIdentifiers().contains(SecurityTokenConstants.KeyIdentifier_NoKeyInfo)) {
            createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyInfo, false, attributes);
            createKeyInfoStructureForSignature(subOutputProcessorChain, wrappingSecurityToken, getSecurityProperties().isUseSingleCert());
            createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyInfo);
        }
        createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Signature);

        // Clean the secret key from memory now that we're done with it
        if (key instanceof Destroyable) {
            try {
                ((Destroyable)key).destroy();
            } catch (DestroyFailedException e) {
                LOG.debug("Error destroying key: {}", e.getMessage());
            }
        }
    }

    protected abstract SignedInfoProcessor newSignedInfoProcessor(
            SignatureAlgorithm signatureAlgorithm, String signatureId,
            XMLSecStartElement xmlSecStartElement, OutputProcessorChain outputProcessorChain)
            throws XMLSecurityException;

    protected abstract void createTransformsStructureForSignature(
            OutputProcessorChain subOutputProcessorChain,
            SignaturePartDef signaturePartDef) throws XMLStreamException, XMLSecurityException;

    protected abstract void createKeyInfoStructureForSignature(
            OutputProcessorChain outputProcessorChain,
            OutboundSecurityToken securityToken,
            boolean useSingleCertificate) throws XMLStreamException, XMLSecurityException;


    protected static class SignedInfoProcessor extends AbstractOutputProcessor {

        private SignerOutputStream signerOutputStream;
        private OutputStream bufferedSignerOutputStream;
        private Transformer transformer;
        private byte[] signatureValue;
        private String inclusiveNamespacePrefixes;
        private SignatureAlgorithm signatureAlgorithm;
        private XMLSecStartElement xmlSecStartElement;
        private String signatureId;

        public SignedInfoProcessor(SignatureAlgorithm signatureAlgorithm, String signatureId, XMLSecStartElement xmlSecStartElement)
                throws XMLSecurityException {
            super();
            this.signatureAlgorithm = signatureAlgorithm;
            this.xmlSecStartElement = xmlSecStartElement;
            this.signatureId = signatureId;
        }

        @Override
        public void init(OutputProcessorChain outputProcessorChain) throws XMLSecurityException {

            this.signerOutputStream = new SignerOutputStream(this.signatureAlgorithm);
            this.bufferedSignerOutputStream = new UnsyncBufferedOutputStream(this.signerOutputStream);

            final String canonicalizationAlgorithm = getSecurityProperties().getSignatureCanonicalizationAlgorithm();

            Map<String, Object> transformerProperties = null;
            if (getSecurityProperties().isAddExcC14NInclusivePrefixes() &&
                    XMLSecurityConstants.NS_C14N_EXCL.equals(canonicalizationAlgorithm)) {

                Set<String> prefixSet = XMLSecurityUtils.getExcC14NInclusiveNamespacePrefixes(xmlSecStartElement, false);
                StringBuilder prefixes = new StringBuilder();
                for (String prefix : prefixSet) {
                    if (prefixes.length() != 0) {
                        prefixes.append(' ');
                    }
                    prefixes.append(prefix);
                }
                this.inclusiveNamespacePrefixes = prefixes.toString();

                transformerProperties = new HashMap<>(2);
                transformerProperties.put(
                        Canonicalizer20010315_Excl.INCLUSIVE_NAMESPACES_PREFIX_LIST, new ArrayList<>(prefixSet));
            }

            this.transformer = XMLSecurityUtils.getTransformer(null, this.bufferedSignerOutputStream,
                    transformerProperties, canonicalizationAlgorithm, XMLSecurityConstants.DIRECTION.OUT);

            super.init(outputProcessorChain);
        }

        public byte[] getSignatureValue() throws XMLSecurityException {
            if (signatureValue != null) {
                return signatureValue;
            }
            try {
                transformer.doFinal();
                bufferedSignerOutputStream.close();
                signatureValue = signerOutputStream.sign();
                return signatureValue;
            } catch (IOException | XMLStreamException e) {
                throw new XMLSecurityException(e);
            }
        }

        public String getSignatureId() {
            return signatureId;
        }

        public String getInclusiveNamespacePrefixes() {
            return inclusiveNamespacePrefixes;
        }

        @Override
        public void processEvent(XMLSecEvent xmlSecEvent, OutputProcessorChain outputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            transformer.transform(xmlSecEvent);
            outputProcessorChain.processEvent(xmlSecEvent);
        }
    }
}
