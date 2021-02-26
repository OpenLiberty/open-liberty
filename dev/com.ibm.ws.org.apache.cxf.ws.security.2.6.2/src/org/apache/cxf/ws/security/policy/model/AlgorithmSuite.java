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
package org.apache.cxf.ws.security.policy.model;

import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.WSSPolicyException;

public class AlgorithmSuite extends AbstractSecurityAssertion {
    private static final Logger LOG = LogUtils.getL7dLogger(AlgorithmSuite.class);
    
    protected String algoSuiteString;

    protected String symmetricSignature = SPConstants.HMAC_SHA1;

    protected String asymmetricSignature = SPConstants.RSA_SHA1;

    protected String computedKey = SPConstants.P_SHA1;

    protected int maximumSymmetricKeyLength = 256;

    protected int minimumAsymmetricKeyLength = 1024;

    protected int maximumAsymmetricKeyLength = 4096;

    protected String digest;

    protected String encryption;

    protected String symmetricKeyWrap;

    protected String asymmetricKeyWrap;

    protected String encryptionKeyDerivation;

    protected int encryptionDerivedKeyLength;

    protected String signatureKeyDerivation;

    protected int signatureDerivedKeyLength;

    protected int minimumSymmetricKeyLength;

    protected String c14n = SPConstants.EX_C14N;

    protected String soapNormalization;

    protected String strTransform;

    protected String xPath;

    public AlgorithmSuite(SPConstants version) {
        super(version);
    }



    public AlgorithmSuite() {
        super(SP12Constants.INSTANCE);
    }



    /**
     * @return Returns the asymmetricKeyWrap.
     */
    public String getAsymmetricKeyWrap() {
        return asymmetricKeyWrap;
    }

    /**
     * @return Returns the asymmetricSignature.
     */
    public String getAsymmetricSignature() {
        return asymmetricSignature;
    }

    /**
     * @param method The asymmetricSignature to set.
     */
    public void setAsymmetricSignature(String method) {
        asymmetricSignature = method;
    }

    /**
     * @return Returns the computedKey.
     */
    public String getComputedKey() {
        return computedKey;
    }

    /**
     * @return Returns the digest.
     */
    public String getDigest() {
        return digest;
    }

    /**
     * @return Returns the encryption.
     */
    public String getEncryption() {
        return encryption;
    }

    /**
     * @return Returns the encryptionKeyDerivation.
     */
    public String getEncryptionKeyDerivation() {
        return encryptionKeyDerivation;
    }

    /**
     * @return Returns the maximumAsymmetricKeyLength.
     */
    public int getMaximumAsymmetricKeyLength() {
        return maximumAsymmetricKeyLength;
    }

    /**
     * @return Returns the maximumSymmetricKeyLength.
     */
    public int getMaximumSymmetricKeyLength() {
        return maximumSymmetricKeyLength;
    }

    /**
     * @return Returns the minimumAsymmetricKeyLength.
     */
    public int getMinimumAsymmetricKeyLength() {
        return minimumAsymmetricKeyLength;
    }

    /**
     * @return Returns the minimumSymmetricKeyLength.
     */
    public int getMinimumSymmetricKeyLength() {
        return minimumSymmetricKeyLength;
    }

    /**
     * @return Returns the signatureKeyDerivation.
     */
    public String getSignatureKeyDerivation() {
        return signatureKeyDerivation;
    }

    /**
     * @return Returns the symmetricKeyWrap.
     */
    public String getSymmetricKeyWrap() {
        return symmetricKeyWrap;
    }

    /**
     * @return Returns the symmetricSignature.
     */
    public String getSymmetricSignature() {
        return symmetricSignature;
    }

    /**
     * @param method The symmetricSignature to set.
     */
    public void setSymmetricSignature(String method) {
        symmetricSignature = method;
    }

    /**
     * @return Returns the c14n.
     */
    public String getInclusiveC14n() {
        return c14n;
    }

    /**
     * @param c14n The c14n to set.
     */
    public void setC14n(String c14n) {
        this.c14n = c14n;
    }

    /**
     * @return Returns the soapNormalization.
     */
    public String getSoapNormalization() {
        return soapNormalization;
    }

    /**
     * @param soapNormalization The soapNormalization to set.
     */
    public void setSoapNormalization(String soapNormalization) {
        this.soapNormalization = soapNormalization;
    }

    /**
     * @return Returns the strTransform.
     */
    public String getStrTransform() {
        return strTransform;
    }

    /**
     * @param strTransform The strTransform to set.
     */
    public void setStrTransform(String strTransform) {
        this.strTransform = strTransform;
    }

    /**
     * @return Returns the xPath.
     */
    public String getXPath() {
        return xPath;
    }

    /**
     * @param path The xPath to set.
     */
    public void setXPath(String path) {
        xPath = path;
    }

    private void setAlgoSuiteString(String algoSuiteString) {
        this.algoSuiteString = algoSuiteString;
    }

    private String getAlgoSuiteString() {
        return algoSuiteString;
    }

    public QName getName() {
        return SP12Constants.INSTANCE.getAlgorithmSuite();
    }
    public QName getRealName() {
        return constants.getAlgorithmSuite();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getRealName().getLocalPart();
        String namespaceURI = getRealName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getRealName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        writer.writeStartElement(prefix, localName, namespaceURI);
        writer.writeNamespace(prefix, namespaceURI);

        // <wsp:Policy>
        String wspPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (wspPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, SPConstants.POLICY.getNamespaceURI());
        }
        writer.writeStartElement(wspPrefix,
                                 SPConstants.POLICY.getLocalPart(),
                                 SPConstants.POLICY.getNamespaceURI());

        //
        writer.writeStartElement(prefix, getAlgoSuiteString(), namespaceURI);
        writer.writeEndElement();

        if (SPConstants.C14N.equals(getInclusiveC14n())) {
            writer.writeStartElement(prefix, SPConstants.INCLUSIVE_C14N, namespaceURI);
            writer.writeEndElement();
        }

        if (SPConstants.SNT.equals(getSoapNormalization())) {
            writer.writeStartElement(prefix, SPConstants.SOAP_NORMALIZATION_10, namespaceURI);
            writer.writeEndElement();
        }

        if (SPConstants.STRT10.equals(getStrTransform())) {
            writer.writeStartElement(prefix, SPConstants.STR_TRANSFORM_10, namespaceURI);
            writer.writeEndElement();
        }

        if (SPConstants.XPATH.equals(getXPath())) {
            writer.writeStartElement(prefix, SPConstants.XPATH10, namespaceURI);
            writer.writeEndElement();
        }

        if (SPConstants.XPATH20.equals(getXPath())) {
            writer.writeStartElement(prefix, SPConstants.XPATH_FILTER20, namespaceURI);
            writer.writeEndElement();
        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:AlgorithmSuite>
        writer.writeEndElement();
    }

    public int getEncryptionDerivedKeyLength() {
        return encryptionDerivedKeyLength;
    }

    public int getSignatureDerivedKeyLength() {
        return signatureDerivedKeyLength;
    }

    public void setAsymmetricKeyWrap(String asymmetricKeyWrap) {
        this.asymmetricKeyWrap = asymmetricKeyWrap;
    }
    
    
    /**
     * Set the algorithm suite
     * 
     * @param algoSuite
     * @throws WSSPolicyException
     * @see SPConstants#ALGO_SUITE_BASIC128
     * @see SPConstants#ALGO_SUITE_BASIC128_RSA15
     * @see SPConstants#ALGO_SUITE_BASIC128_SHA256
     * @see SPConstants#ALGO_SUITE_BASIC128_SHA256_RSA15
     * @see SPConstants#ALGO_SUITE_BASIC192
     * @see SPConstants#ALGO_SUITE_BASIC192_RSA15
     * @see SPConstants#ALGO_SUITE_BASIC192_SHA256
     * @see SPConstants#ALGO_SUITE_BASIC192_SHA256_RSA15
     * @see SPConstants#ALGO_SUITE_BASIC256
     * @see SPConstants#ALGO_SUITE_BASIC256_RSA15
     * @see SPConstants#ALGO_SUITE_BASIC256_SHA256
     * @see SPConstants#ALGO_SUITE_BASIC256_SHA256_RSA15
     * @see SPConstants#ALGO_SUITE_TRIPLE_DES
     * @see SPConstants#ALGO_SUITE_TRIPLE_DES_RSA15
     * @see SPConstants#ALGO_SUITE_TRIPLE_DES_SHA256
     * @see SPConstants#ALGO_SUITE_TRIPLE_DES_SHA256_RSA15
     */
    //CHECKSTYLE:OFF
    public void setAlgorithmSuite(String algoSuite) throws WSSPolicyException {
        setAlgoSuiteString(algoSuite);
        this.algoSuiteString = algoSuite;

        // TODO: Optimize this :-)
        if (SPConstants.ALGO_SUITE_BASIC256.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES256;
            this.symmetricKeyWrap = SPConstants.KW_AES256;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L256;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 256;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 256;
            this.encryptionDerivedKeyLength = 256;
        } else if (SPConstants.ALGO_SUITE_BASIC192.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES192;
            this.symmetricKeyWrap = SPConstants.KW_AES192;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC128.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES128;
            this.symmetricKeyWrap = SPConstants.KW_AES128;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L128;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L128;
            this.encryptionDerivedKeyLength = 128;
            this.signatureDerivedKeyLength = 128;
            this.minimumSymmetricKeyLength = 128;
        } else if (SPConstants.ALGO_SUITE_TRIPLE_DES.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.TRIPLE_DES;
            this.symmetricKeyWrap = SPConstants.KW_TRIPLE_DES;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC256_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES256;
            this.symmetricKeyWrap = SPConstants.KW_AES256;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L256;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 256;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 256;
        } else if (SPConstants.ALGO_SUITE_BASIC192_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES192;
            this.symmetricKeyWrap = SPConstants.KW_AES192;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC128_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.AES128;
            this.symmetricKeyWrap = SPConstants.KW_AES128;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L128;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L128;
            this.encryptionDerivedKeyLength = 128;
            this.signatureDerivedKeyLength = 128;
            this.minimumSymmetricKeyLength = 128;
        } else if (SPConstants.ALGO_SUITE_TRIPLE_DES_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA1;
            this.encryption = SPConstants.TRIPLE_DES;
            this.symmetricKeyWrap = SPConstants.KW_TRIPLE_DES;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC256_SHA256.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES256;
            this.symmetricKeyWrap = SPConstants.KW_AES256;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L256;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 256;
            this.signatureDerivedKeyLength = 256;
            this.minimumSymmetricKeyLength = 256;
        } else if (SPConstants.ALGO_SUITE_BASIC192_SHA256.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES192;
            this.symmetricKeyWrap = SPConstants.KW_AES192;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC128_SHA256.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES128;
            this.symmetricKeyWrap = SPConstants.KW_AES128;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L128;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L128;
            this.encryptionDerivedKeyLength = 128;
            this.signatureDerivedKeyLength = 128;
            this.minimumSymmetricKeyLength = 128;
        } else if (SPConstants.ALGO_SUITE_TRIPLE_DES_SHA256.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.TRIPLE_DES;
            this.symmetricKeyWrap = SPConstants.KW_TRIPLE_DES;
            this.asymmetricKeyWrap = SPConstants.KW_RSA_OAEP;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC256_SHA256_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES256;
            this.symmetricKeyWrap = SPConstants.KW_AES256;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L256;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 256;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 256;
        } else if (SPConstants.ALGO_SUITE_BASIC192_SHA256_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES192;
            this.symmetricKeyWrap = SPConstants.KW_AES192;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else if (SPConstants.ALGO_SUITE_BASIC128_SHA256_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.AES128;
            this.symmetricKeyWrap = SPConstants.KW_AES128;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L128;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L128;
            this.encryptionDerivedKeyLength = 128;
            this.signatureDerivedKeyLength = 128;
            this.minimumSymmetricKeyLength = 128;
        } else if (SPConstants.ALGO_SUITE_TRIPLE_DES_SHA256_RSA15.equals(algoSuite)) {
            this.digest = SPConstants.SHA256;
            this.encryption = SPConstants.TRIPLE_DES;
            this.symmetricKeyWrap = SPConstants.KW_TRIPLE_DES;
            this.asymmetricKeyWrap = SPConstants.KW_RSA15;
            this.encryptionKeyDerivation = SPConstants.P_SHA1_L192;
            this.signatureKeyDerivation = SPConstants.P_SHA1_L192;
            this.encryptionDerivedKeyLength = 192;
            this.signatureDerivedKeyLength = 192;
            this.minimumSymmetricKeyLength = 192;
        } else {
            throw new WSSPolicyException(new Message("INVALID_ALGORITHM", LOG, algoSuite));
        }
    }
}
