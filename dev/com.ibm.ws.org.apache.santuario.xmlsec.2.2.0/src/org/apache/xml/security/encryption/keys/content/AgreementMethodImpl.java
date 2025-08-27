/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption.keys.content;

import org.apache.xml.security.encryption.AgreementMethod;
import org.apache.xml.security.encryption.KeyDerivationMethod;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.encryption.keys.OriginatorKeyInfo;
import org.apache.xml.security.encryption.keys.RecipientKeyInfo;
import org.apache.xml.security.encryption.keys.content.derivedKey.ConcatKDFParamsImpl;
import org.apache.xml.security.encryption.keys.content.derivedKey.HKDFParamsImpl;
import org.apache.xml.security.encryption.keys.content.derivedKey.KDFParams;
import org.apache.xml.security.encryption.keys.content.derivedKey.KeyDerivationMethodImpl;
import org.apache.xml.security.encryption.params.ConcatKDFParams;
import org.apache.xml.security.encryption.params.HKDFParams;
import org.apache.xml.security.encryption.params.KeyAgreementParameters;
import org.apache.xml.security.encryption.params.KeyDerivationParameters;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.KeyInfoContent;
import org.apache.xml.security.utils.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * The implementation of the AgreementMethod interface. The element contains information about
 * the key agreement algorithm for deriving the encryption key.
 */
public class AgreementMethodImpl extends EncryptionElementProxy implements KeyInfoContent, AgreementMethod {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(AgreementMethodImpl.class);

    private byte[] kaNonce;
    private List<Element> agreementMethodInformation;
    private KeyDerivationMethod keyDerivationMethod;
    private OriginatorKeyInfo originatorKeyInfo;
    private RecipientKeyInfo recipientKeyInfo;
    private String algorithmURI;

    /**
     * Constructor AgreementMethodImpl for generating AgreementMethod from scratch based on {@link KeyAgreementParameters}.
     * The constructor generates {@link KeyDerivationMethod} if given and {@link OriginatorKeyInfo} based on originator
     * public key for ECDH-ES key agreement. It generates a placeholder element for RecipientKeyInfo. The recipient key info value
     * must be set later.
     *
     * @param doc                   the {@link Document} in which <code>AgreementMethod</code> will be placed
     * @param keyAgreementParameter the {@link KeyAgreementParameters} from which <code>AgreementMethod</code> will be generated
     * @throws XMLEncryptionException if the Key derivation algorithm is not supported or invalid parameters are given.
     */
    public AgreementMethodImpl(Document doc, KeyAgreementParameters keyAgreementParameter) throws XMLEncryptionException {
        this(doc, keyAgreementParameter.getKeyAgreementAlgorithm());

        if (keyAgreementParameter.getKeyDerivationParameter() != null) {
            KeyDerivationMethod keyDerivationMethod = createKeyDerivationMethod(keyAgreementParameter);
            setKeyDerivationMethod(keyDerivationMethod);
        }
        // if ephemeral static key agreement then add originator public key automatically
        if (keyAgreementParameter.getOriginatorPublicKey() != null) {
            setOriginatorPublicKey(keyAgreementParameter.getOriginatorPublicKey());
        }
        // set recipient key info holder
        RecipientKeyInfo recipientKeyInfo = new RecipientKeyInfo(getDocument());
        setRecipientKeyInfo(recipientKeyInfo);
    }

    /**
     * Constructor AgreementMethodImpl for generating AgreementMethod from scratch based on algorithm URI. The constructor
     * builds a placeholder element for {@link KeyDerivationMethod}, {@link OriginatorKeyInfo} and {@link RecipientKeyInfo}. The values for these elements
     * must be set later.
     *
     * @param algorithm the algorithm URI for the key agreement algorithm
     */
    public AgreementMethodImpl(Document doc, String algorithm) {
        super(doc);

        agreementMethodInformation = new LinkedList<>();
        URI tmpAlgorithm;
        try {
            tmpAlgorithm = new URI(algorithm);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Algorithm [" + algorithm + "] is not URI ", ex);
        }
        algorithmURI = tmpAlgorithm.toString();

        setLocalAttribute(Constants._ATT_ALGORITHM, algorithmURI);
    }

    /**
     * Constructor AgreementMethodImpl based on XML {@link Element}.
     *
     * @param element the XML {@link Element} containing AgreementMethod information
     * @throws XMLSecurityException if the AgreementMethod element has invalid XML structure
     */
    public AgreementMethodImpl(Element element) throws XMLSecurityException {
        super(element, EncryptionConstants.EncryptionSpecNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getKANonce() {
        return kaNonce;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKANonce(byte[] kanonce) {
        kaNonce = kanonce;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Element> getAgreementMethodInformation() {
        return agreementMethodInformation.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAgreementMethodInformation(Element info) {
        agreementMethodInformation.add(info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAgreementMethodInformation(Element info) {
        agreementMethodInformation.remove(info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyDerivationMethod getKeyDerivationMethod() throws XMLSecurityException {

        if (keyDerivationMethod != null) {
            LOG.debug("Returning cached KeyDerivationMethod");
            return keyDerivationMethod;
        }

        Element keyDerivationMethodElement =
                XMLUtils.selectXenc11Node(getElement().getFirstChild(), EncryptionConstants._TAG_KEYDERIVATIONMETHOD, 0);

        if (keyDerivationMethodElement == null) {
            LOG.debug("No KeyDerivationMethod element found!");
            return null;
        }
        keyDerivationMethod = new KeyDerivationMethodImpl(keyDerivationMethodElement, baseURI);
        return keyDerivationMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeyDerivationMethod(KeyDerivationMethod keyDerivationMethod) {
        this.keyDerivationMethod = keyDerivationMethod;
        if (keyDerivationMethod instanceof ElementProxy) {
            appendSelf((ElementProxy) keyDerivationMethod);
            addReturnToSelf();
        } else {
            LOG.debug("KeyDerivationMethod " + keyDerivationMethod +  " is set but is not an instance of ElementProxy. " +
                    "The DOM node is lost upon serialization.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OriginatorKeyInfo getOriginatorKeyInfo() throws XMLSecurityException {
        if (originatorKeyInfo != null) {
            LOG.debug("Returning cached OriginatorKeyInfo");
            return originatorKeyInfo;
        }

        Element originatorKeyInfoElement =
                XMLUtils.selectXencNode(getElement().getFirstChild(), EncryptionConstants._TAG_ORIGINATORKEYINFO, 0);

        if (originatorKeyInfoElement == null) {
            LOG.debug("No OriginatorKeyInfo element found!");
            return null;
        }
        originatorKeyInfo = new OriginatorKeyInfo(originatorKeyInfoElement, baseURI);

        return originatorKeyInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOriginatorKeyInfo(OriginatorKeyInfo keyInfo) {
        originatorKeyInfo = keyInfo;
        appendSelf(keyInfo);
        addReturnToSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOriginatorPublicKey(PublicKey publicKey) {
        OriginatorKeyInfo originatorKeyInfo = new OriginatorKeyInfo(getDocument());
        originatorKeyInfo.add(publicKey);
        setOriginatorKeyInfo(originatorKeyInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecipientKeyInfo getRecipientKeyInfo() throws XMLSecurityException {
        if (recipientKeyInfo != null) {
            LOG.debug("Returning cached RecipientKeyInfo");
            return recipientKeyInfo;
        }

        Element recipientKeyInfoElement =
                XMLUtils.selectXencNode(getElement().getFirstChild(), EncryptionConstants._TAG_RECIPIENTKEYINFO, 0);

        if (recipientKeyInfoElement == null) {
            LOG.debug("No RecipientKeyInfo element found!");
            return null;
        }
        recipientKeyInfo = new RecipientKeyInfo(recipientKeyInfoElement, baseURI);

        return recipientKeyInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRecipientKeyInfo(RecipientKeyInfo keyInfo) {
        recipientKeyInfo = keyInfo;
        appendSelf(keyInfo);
        addReturnToSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlgorithm() {
        if (algorithmURI == null) {
            algorithmURI = getLocalAttribute(Constants._ATT_ALGORITHM);
        }
        return algorithmURI;
    }

    @Override
    public String getBaseLocalName() {
        return EncryptionConstants._TAG_AGREEMENTMETHOD;
    }

    /**
     * Method creates a {@link KeyDerivationMethod} element based on parameters: {@link KeyAgreementParameters}.
     * The method supports parameter types {@link ConcatKDFParams} and {@link HKDFParams}.
     *
     * @param keyAgreementParameter the {@link KeyAgreementParameters} from which {@link KeyDerivationMethod} will be generated.
     * @return the generated {@link KeyDerivationMethod} element based on {@link KeyAgreementParameters}
     * @throws XMLEncryptionException   if the Key derivation algorithm is not supported or invalid parameters are given.
     * @throws IllegalArgumentException if KeyAgreementParameters is missing or wrong class type for derivation algorithm
     */
    private KeyDerivationMethod createKeyDerivationMethod(KeyAgreementParameters keyAgreementParameter) throws XMLEncryptionException {
        KeyDerivationParameters kdfParameters = keyAgreementParameter.getKeyDerivationParameter();
        if (kdfParameters == null) {
            throw new IllegalArgumentException("KeyAgreementParameters must have KeyDerivationParameters set!");
        }

        KeyDerivationMethodImpl keyDerivationMethod = new KeyDerivationMethodImpl(getDocument());
        keyDerivationMethod.setAlgorithm(kdfParameters.getAlgorithm());

        KDFParams kdfParams;
        switch (kdfParameters.getAlgorithm()) {
            case EncryptionConstants.ALGO_ID_KEYDERIVATION_CONCATKDF:
                kdfParams = getConcatKDFParams(kdfParameters);
                break;
            case EncryptionConstants.ALGO_ID_KEYDERIVATION_HKDF:
                kdfParams = getHKDFParams(kdfParameters);
                break;
            default:
                throw new XMLEncryptionException("KeyDerivation.UnsupportedAlgorithm",
                        kdfParameters.getAlgorithm(), kdfParameters.getClass().getName());
        }

        keyDerivationMethod.setKDFParams(kdfParams);
        return keyDerivationMethod;
    }

    /**
     * Method creates a {@link ConcatKDFParamsImpl} based on {@link ConcatKDFParams}.
     *
     * @param parameter the {@link KeyDerivationParameters} from which {@link ConcatKDFParamsImpl} will be generated.
     * @return the {@link ConcatKDFParamsImpl}
     * @throws IllegalArgumentException if parameter is not instance of {@link ConcatKDFParams}
     */
    private ConcatKDFParamsImpl getConcatKDFParams(KeyDerivationParameters parameter) {

        if (!(parameter instanceof ConcatKDFParams)) {
            throw new IllegalArgumentException("KDF Parameter must be instance of ConcatKDFParams");
        }

        ConcatKDFParams kdfParameters = (ConcatKDFParams) parameter;
        ConcatKDFParamsImpl concatKDFParams = new ConcatKDFParamsImpl(getDocument());
        concatKDFParams.setDigestMethod(kdfParameters.getDigestAlgorithm());
        // set parameters
        concatKDFParams.setAlgorithmId(kdfParameters.getAlgorithmID());
        concatKDFParams.setPartyUInfo(kdfParameters.getPartyUInfo());
        concatKDFParams.setPartyVInfo(kdfParameters.getPartyVInfo());
        concatKDFParams.setSuppPubInfo(kdfParameters.getSuppPubInfo());
        concatKDFParams.setSuppPrivInfo(kdfParameters.getSuppPrivInfo());
        return concatKDFParams;
    }

    /**
     * Method creates a {@link HKDFParamsImpl} based on {@link HKDFParams}.
     *
     * @param parameter the {@link KeyDerivationParameters} containing HKDF parameters.
     * @return the {@link HKDFParamsImpl}
     * @throws IllegalArgumentException if parameter is not instance of {@link HKDFParams}
     */
    private HKDFParamsImpl getHKDFParams(KeyDerivationParameters parameter) {

        if (!(parameter instanceof HKDFParams)) {
            throw new IllegalArgumentException("KDF Parameter must be instance of HKDFParams");
        }
        HKDFParams kdfParameters = (HKDFParams) parameter;

        HKDFParamsImpl kdfParams = new HKDFParamsImpl(getDocument());
        kdfParams.setPRFAlgorithm(kdfParameters.getHmacHashAlgorithm());
        Base64.Encoder base64Encoder = Base64.getEncoder();
        if (kdfParameters.getSalt() != null) {
            kdfParams.setSalt(base64Encoder.encodeToString(kdfParameters.getSalt()));
        }
        if (kdfParameters.getInfo() != null) {
            kdfParams.setInfo(base64Encoder.encodeToString(kdfParameters.getInfo()));
        }
        // set parameters
        kdfParams.setKeyLength(kdfParameters.getKeyBitLength() / 8);
        return kdfParams;
    }
}
