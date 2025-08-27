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
package org.apache.wss4j.policy.model;

import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyContainingAssertion;
import org.apache.wss4j.policy.SPConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

public class AlgorithmSuite extends AbstractSecurityAssertion implements PolicyContainingAssertion {

    protected static final Map<String, AlgorithmSuiteType> ALGORITHM_SUITE_TYPES = new HashMap<>();

    private static final int MAX_SKL = 256;
    private static final int MIN_AKL = 1024;
    private static final int MAX_AKL = 4096;

    static {
        ALGORITHM_SUITE_TYPES.put("Basic256", new AlgorithmSuiteType(
                "Basic256",
                SPConstants.SHA1,
                SPConstants.AES256,
                SPConstants.KW_AES256,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L256,
                SPConstants.P_SHA1_L192,
                256, 192, 256,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic192", new AlgorithmSuiteType(
                "Basic192",
                SPConstants.SHA1,
                SPConstants.AES192,
                SPConstants.KW_AES192,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic128", new AlgorithmSuiteType(
                "Basic128",
                SPConstants.SHA1,
                SPConstants.AES128,
                SPConstants.KW_AES128,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L128,
                SPConstants.P_SHA1_L128,
                128, 128, 128,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("TripleDes", new AlgorithmSuiteType(
                "TripleDes",
                SPConstants.SHA1,
                SPConstants.TRIPLE_DES,
                SPConstants.KW_TRIPLE_DES,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic256Rsa15", new AlgorithmSuiteType(
                "Basic256Rsa15",
                SPConstants.SHA1,
                SPConstants.AES256,
                SPConstants.KW_AES256,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L256,
                SPConstants.P_SHA1_L192,
                256, 192, 256,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic192Rsa15", new AlgorithmSuiteType(
                "Basic192Rsa15",
                SPConstants.SHA1,
                SPConstants.AES192,
                SPConstants.KW_AES192,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic128Rsa15", new AlgorithmSuiteType(
                "Basic128Rsa15",
                SPConstants.SHA1,
                SPConstants.AES128,
                SPConstants.KW_AES128,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L128,
                SPConstants.P_SHA1_L128,
                128, 128, 128,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("TripleDesRsa15", new AlgorithmSuiteType(
                "TripleDesRsa15",
                SPConstants.SHA1,
                SPConstants.TRIPLE_DES,
                SPConstants.KW_TRIPLE_DES,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic256Sha256", new AlgorithmSuiteType(
                "Basic256Sha256",
                SPConstants.SHA256,
                SPConstants.AES256,
                SPConstants.KW_AES256,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L256,
                SPConstants.P_SHA1_L192,
                256, 192, 256,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic192Sha256", new AlgorithmSuiteType(
                "Basic192Sha256",
                SPConstants.SHA256,
                SPConstants.AES192,
                SPConstants.KW_AES192,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic128Sha256", new AlgorithmSuiteType(
                "Basic128Sha256",
                SPConstants.SHA256,
                SPConstants.AES128,
                SPConstants.KW_AES128,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L128,
                SPConstants.P_SHA1_L128,
                128, 128, 128,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("TripleDesSha256", new AlgorithmSuiteType(
                "TripleDesSha256",
                SPConstants.SHA256,
                SPConstants.TRIPLE_DES,
                SPConstants.KW_TRIPLE_DES,
                SPConstants.KW_RSA_OAEP,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic256Sha256Rsa15", new AlgorithmSuiteType(
                "Basic256Sha256Rsa15",
                SPConstants.SHA256,
                SPConstants.AES256,
                SPConstants.KW_AES256,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L256,
                SPConstants.P_SHA1_L192,
                256, 192, 256,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic192Sha256Rsa15", new AlgorithmSuiteType(
                "Basic192Sha256Rsa15",
                SPConstants.SHA256,
                SPConstants.AES192,
                SPConstants.KW_AES192,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("Basic128Sha256Rsa15", new AlgorithmSuiteType(
                "Basic128Sha256Rsa15",
                SPConstants.SHA256,
                SPConstants.AES128,
                SPConstants.KW_AES128,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L128,
                SPConstants.P_SHA1_L128,
                128, 128, 128,
                MAX_SKL, MIN_AKL, MAX_AKL));
        ALGORITHM_SUITE_TYPES.put("TripleDesSha256Rsa15", new AlgorithmSuiteType(
                "TripleDesSha256Rsa15",
                SPConstants.SHA256,
                SPConstants.TRIPLE_DES,
                SPConstants.KW_TRIPLE_DES,
                SPConstants.KW_RSA15,
                SPConstants.P_SHA1_L192,
                SPConstants.P_SHA1_L192,
                192, 192, 192,
                MAX_SKL, MIN_AKL, MAX_AKL));
    }

    public static final class AlgorithmSuiteType {

        private String name;
        private String digest;
        private String encryption;
        private String symmetricKeyWrap;
        private String asymmetricKeyWrap;
        private String encryptionKeyDerivation;
        private String signatureKeyDerivation;
        private int encryptionDerivedKeyLength;
        private int signatureDerivedKeyLength;
        private int minimumSymmetricKeyLength;
        private int maximumSymmetricKeyLength;
        private int minimumAsymmetricKeyLength;
        private int maximumAsymmetricKeyLength;
        private String mgfAlgo;
        private String ns;
        private String encryptionDigest;
        private String symmetricSignature = SPConstants.HMAC_SHA1;
        private String asymmetricSignature = SPConstants.RSA_SHA1;

        public AlgorithmSuiteType(String name, String digest, String encryption, String symmetricKeyWrap, //NOPMD
                                  String asymmetricKeyWrap, String encryptionKeyDerivation,
                                  String signatureKeyDerivation, int encryptionDerivedKeyLength,
                                  int signatureDerivedKeyLength, int minimumSymmetricKeyLength,
                                  int maximumSymmetricKeyLength, int minimumAsymmetricKeyLength,
                                  int maximumAsymmetricKeyLength) {
            this(name, digest, encryption, symmetricKeyWrap, asymmetricKeyWrap, encryptionKeyDerivation,
                 signatureKeyDerivation, SPConstants.HMAC_SHA1, SPConstants.RSA_SHA1, encryptionDerivedKeyLength,
                 signatureDerivedKeyLength, minimumSymmetricKeyLength, maximumSymmetricKeyLength,
                 minimumAsymmetricKeyLength, maximumAsymmetricKeyLength);
        }

        public AlgorithmSuiteType(String name, String digest, String encryption, String symmetricKeyWrap, //NOPMD
                                  String asymmetricKeyWrap, String encryptionKeyDerivation,
                                  String signatureKeyDerivation, String symmetricSignature,
                                  String asymmetricSignature, int encryptionDerivedKeyLength,
                                  int signatureDerivedKeyLength, int minimumSymmetricKeyLength,
                                  int maximumSymmetricKeyLength, int minimumAsymmetricKeyLength,
                                  int maximumAsymmetricKeyLength) {
            this.name = name;
            this.digest = digest;
            this.encryption = encryption;
            this.symmetricKeyWrap = symmetricKeyWrap;
            this.asymmetricKeyWrap = asymmetricKeyWrap;
            this.encryptionKeyDerivation = encryptionKeyDerivation;
            this.signatureKeyDerivation = signatureKeyDerivation;
            this.symmetricSignature = symmetricSignature;
            this.asymmetricSignature = asymmetricSignature;
            this.encryptionDerivedKeyLength = encryptionDerivedKeyLength;
            this.signatureDerivedKeyLength = signatureDerivedKeyLength;
            this.minimumSymmetricKeyLength = minimumSymmetricKeyLength;
            this.maximumSymmetricKeyLength = maximumSymmetricKeyLength;
            this.minimumAsymmetricKeyLength = minimumAsymmetricKeyLength;
            this.maximumAsymmetricKeyLength = maximumAsymmetricKeyLength;
        }

        public AlgorithmSuiteType(AlgorithmSuiteType algorithmSuiteType) {
            this.name = algorithmSuiteType.name;
            this.digest = algorithmSuiteType.digest;
            this.encryption = algorithmSuiteType.encryption;
            this.symmetricKeyWrap = algorithmSuiteType.symmetricKeyWrap;
            this.asymmetricKeyWrap = algorithmSuiteType.asymmetricKeyWrap;
            this.encryptionKeyDerivation = algorithmSuiteType.encryptionKeyDerivation;
            this.signatureKeyDerivation = algorithmSuiteType.signatureKeyDerivation;
            this.symmetricSignature = algorithmSuiteType.symmetricSignature;
            this.asymmetricSignature = algorithmSuiteType.asymmetricSignature;
            this.encryptionDerivedKeyLength = algorithmSuiteType.encryptionDerivedKeyLength;
            this.signatureDerivedKeyLength = algorithmSuiteType.signatureDerivedKeyLength;
            this.minimumSymmetricKeyLength = algorithmSuiteType.minimumSymmetricKeyLength;
            this.maximumSymmetricKeyLength = algorithmSuiteType.maximumSymmetricKeyLength;
            this.minimumAsymmetricKeyLength = algorithmSuiteType.minimumAsymmetricKeyLength;
            this.maximumAsymmetricKeyLength = algorithmSuiteType.maximumAsymmetricKeyLength;
            this.mgfAlgo = algorithmSuiteType.mgfAlgo;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }

            if (!(object instanceof AlgorithmSuiteType)) {
                return false;
            }

            AlgorithmSuiteType that = (AlgorithmSuiteType)object;
            if (name != null && !name.equals(that.name)
                || name == null && that.name != null) {
                return false;
            }
            if (digest != null && !digest.equals(that.digest)
                || digest == null && that.digest != null) {
                return false;
            }
            if (encryption != null && !encryption.equals(that.encryption)
                || encryption == null && that.encryption != null) {
                return false;
            }
            if (symmetricKeyWrap != null && !symmetricKeyWrap.equals(that.symmetricKeyWrap)
                || symmetricKeyWrap == null && that.symmetricKeyWrap != null) {
                return false;
            }
            if (asymmetricKeyWrap != null && !asymmetricKeyWrap.equals(that.asymmetricKeyWrap)
                || asymmetricKeyWrap == null && that.asymmetricKeyWrap != null) {
                return false;
            }
            if (encryptionKeyDerivation != null && !encryptionKeyDerivation.equals(that.encryptionKeyDerivation)
                || encryptionKeyDerivation == null && that.encryptionKeyDerivation != null) {
                return false;
            }
            if (signatureKeyDerivation != null && !signatureKeyDerivation.equals(that.signatureKeyDerivation)
                || signatureKeyDerivation == null && that.signatureKeyDerivation != null) {
                return false;
            }
            if (symmetricSignature != null && !symmetricSignature.equals(that.symmetricSignature)
                || symmetricSignature == null && that.symmetricSignature != null) {
                return false;
            }
            if (asymmetricSignature != null && !asymmetricSignature.equals(that.asymmetricSignature)
                || asymmetricSignature == null && that.asymmetricSignature != null) {
                return false;
            }
            if (ns != null && !ns.equals(that.ns)
                || ns == null && that.ns != null) {
                return false;
            }
            if (mgfAlgo != null && !mgfAlgo.equals(that.mgfAlgo)
                || mgfAlgo == null && that.mgfAlgo != null) {
                return false;
            }
            if (encryptionDigest != null && !encryptionDigest.equals(that.encryptionDigest)
                || encryptionDigest == null && that.encryptionDigest != null) {
                return false;
            }

            return !(encryptionDerivedKeyLength != that.encryptionDerivedKeyLength
                || signatureDerivedKeyLength != that.signatureDerivedKeyLength
                || minimumSymmetricKeyLength != that.minimumSymmetricKeyLength
                || maximumSymmetricKeyLength != that.maximumSymmetricKeyLength
                || minimumAsymmetricKeyLength != that.minimumAsymmetricKeyLength
                || maximumAsymmetricKeyLength != that.maximumAsymmetricKeyLength);
        }

        @Override
        public int hashCode() {
            int result = 17;
            if (name != null) {
                result = 31 * result + name.hashCode();
            }
            if (digest != null) {
                result = 31 * result + digest.hashCode();
            }
            if (encryption != null) {
                result = 31 * result + encryption.hashCode();
            }
            if (symmetricKeyWrap != null) {
                result = 31 * result + symmetricKeyWrap.hashCode();
            }
            if (asymmetricKeyWrap != null) {
                result = 31 * result + asymmetricKeyWrap.hashCode();
            }
            if (encryptionKeyDerivation != null) {
                result = 31 * result + encryptionKeyDerivation.hashCode();
            }
            if (signatureKeyDerivation != null) {
                result = 31 * result + signatureKeyDerivation.hashCode();
            }
            if (symmetricSignature != null) {
                result = 31 * result + symmetricSignature.hashCode();
            }
            if (asymmetricSignature != null) {
                result = 31 * result + asymmetricSignature.hashCode();
            }

            result = 31 * result + Integer.hashCode(encryptionDerivedKeyLength);
            result = 31 * result + Integer.hashCode(signatureDerivedKeyLength);
            result = 31 * result + Integer.hashCode(minimumSymmetricKeyLength);
            result = 31 * result + Integer.hashCode(maximumSymmetricKeyLength);
            result = 31 * result + Integer.hashCode(minimumAsymmetricKeyLength);
            result = 31 * result + Integer.hashCode(maximumAsymmetricKeyLength);

            if (mgfAlgo != null) {
                result = 31 * result + mgfAlgo.hashCode();
            }
            if (ns != null) {
                result = 31 * result + ns.hashCode();
            }
            if (encryptionDigest != null) {
                result = 31 * result + encryptionDigest.hashCode();
            }

            return 31 * result + super.hashCode();
        }

        public String getName() {
            return name;
        }

        public String getDigest() {
            return digest;
        }
        
        public void setDigest(String digest) {
            this.digest = digest;
        }

        public String getEncryption() {
            return encryption;
        }
        
        public void setEncryption(String encryption) {
            this.encryption = encryption;
        }

        public String getSymmetricKeyWrap() {
            return symmetricKeyWrap;
        }
        
        public void setSymmetricKeyWrap(String symmetricKeyWrap) {
            this.symmetricKeyWrap = symmetricKeyWrap;
        }

        public String getAsymmetricKeyWrap() {
            return asymmetricKeyWrap;
        }
        
        public void setAsymmetricKeyWrap(String asymmetricKeyWrap) {
            this.asymmetricKeyWrap = asymmetricKeyWrap;
        }

        public String getEncryptionKeyDerivation() {
            return encryptionKeyDerivation;
        }
        
        public void setEncryptionKeyDerivation(String encryptionKeyDerivation) {
            this.encryptionKeyDerivation = encryptionKeyDerivation;
        }

        public String getSignatureKeyDerivation() {
            return signatureKeyDerivation;
        }
        
        public void setSignatureKeyDerivation(String signatureKeyDerivation) {
            this.signatureKeyDerivation = signatureKeyDerivation;
        }

        public String getSymmetricSignature() {
            return symmetricSignature;
        }

        public String getAsymmetricSignature() {
            return asymmetricSignature;
        }

        public void setSymmetricSignature(String symmetricSignature) {
            this.symmetricSignature = symmetricSignature;
        }

        public void setAsymmetricSignature(String asymmetricSignature) {
            this.asymmetricSignature = asymmetricSignature;
        }

        public int getEncryptionDerivedKeyLength() {
            return encryptionDerivedKeyLength;
        }
        
        public void getEncryptionDerivedKeyLength(int encryptionDerivedKeyLength) {
            this.encryptionDerivedKeyLength = encryptionDerivedKeyLength;
        }

        public int getSignatureDerivedKeyLength() {
            return signatureDerivedKeyLength;
        }
        
        public void setSignatureDerivedKeyLength(int signatureDerivedKeyLength) {
            this.signatureDerivedKeyLength = signatureDerivedKeyLength;
        }

        public int getMinimumSymmetricKeyLength() {
            return minimumSymmetricKeyLength;
        }
        
        public void setMinimumSymmetricKeyLength(int minimumSymmetricKeyLength) {
            this.minimumSymmetricKeyLength = minimumSymmetricKeyLength;
        }

        public int getMaximumSymmetricKeyLength() {
            return maximumSymmetricKeyLength;
        }

        public void setMaximumSymmetricKeyLength(int maximumSymmetricKeyLength) {
            this.maximumSymmetricKeyLength = maximumSymmetricKeyLength;
        }
        
        public int getMinimumAsymmetricKeyLength() {
            return minimumAsymmetricKeyLength;
        }
        
        public void setMinimumAsymmetricKeyLength(int minimumAsymmetricKeyLength) {
            this.minimumAsymmetricKeyLength = minimumAsymmetricKeyLength;
        }

        public int getMaximumAsymmetricKeyLength() {
            return maximumAsymmetricKeyLength;
        }
        
        public void setMaximumAsymmetricKeyLength(int maximumAsymmetricKeyLength) {
            this.maximumAsymmetricKeyLength = maximumAsymmetricKeyLength;
        }

        public void setNamespace(String ns) {
            this.ns = ns;
        }

        public String getNamespace() {
            return ns;
        }

        public void setMGFAlgo(String mgfAlgo) {
            this.mgfAlgo = mgfAlgo;
        }

        public String getMGFAlgo() {
            return mgfAlgo;
        }

        public void setEncryptionDigest(String encryptionDigest) {
            this.encryptionDigest = encryptionDigest;
        }

        public String getEncryptionDigest() {
            return encryptionDigest;
        }
    }

    public enum XPathType {
        XPathNone(null),
        XPath10(SPConstants.XPATH),
        XPathFilter20(SPConstants.XPATH20),
        AbsXPath(SPConstants.ABS_XPATH);

        private static final Map<String, XPathType> LOOKUP = new HashMap<>();

        static {
            for (XPathType u : EnumSet.allOf(XPathType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        public static XPathType lookUp(String name) {
            return LOOKUP.get(name);
        }

        private String value;

        public String getValue() {
            return value;
        }

        XPathType(String value) {
            this.value = value;
        }
    }

    public enum C14NType {
        ExclusiveC14N(SPConstants.EX_C14N),
        InclusiveC14N(SPConstants.C14N),
        InclusiveC14N11(SPConstants.C14N11);

        private static final Map<String, C14NType> LOOKUP = new HashMap<>();

        static {
            for (C14NType u : EnumSet.allOf(C14NType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        private String value;

        public static C14NType lookUp(String name) {
            return LOOKUP.get(name);
        }

        public String getValue() {
            return value;
        }

        C14NType(String value) {
            this.value = value;
        }
    }

    public enum SOAPNormType {
        SOAPNormalizationNone(null),
        SOAPNormalization10(SPConstants.SOAP_NORMALIZATION_10);

        private static final Map<String, SOAPNormType> LOOKUP = new HashMap<>();

        static {
            for (SOAPNormType u : EnumSet.allOf(SOAPNormType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        public static SOAPNormType lookUp(String name) {
            return LOOKUP.get(name);
        }

        private String value;

        public String getValue() {
            return value;
        }

        SOAPNormType(String value) {
            this.value = value;
        }
    }

    public enum STRType {
        STRTransformNone(null),
        STRTransform10(SPConstants.STR_TRANSFORM_10);

        private static final Map<String, STRType> LOOKUP = new HashMap<>();

        static {
            for (STRType u : EnumSet.allOf(STRType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        public static STRType lookUp(String name) {
            return LOOKUP.get(name);
        }

        private String value;

        public String getValue() {
            return value;
        }

        STRType(String value) {
            this.value = value;
        }
    }

    private Policy nestedPolicy;
    private AlgorithmSuiteType algorithmSuiteType;
    private C14NType c14n = C14NType.ExclusiveC14N;
    private SOAPNormType soapNormType = SOAPNormType.SOAPNormalizationNone;
    private STRType strType = STRType.STRTransformNone;
    private XPathType xPathType = XPathType.XPathNone;

    private String computedKey = SPConstants.P_SHA1;
    private String firstInvalidAlgorithmSuite;

    public AlgorithmSuite(SPConstants.SPVersion version, Policy nestedPolicy) {
        super(version);
        this.nestedPolicy = nestedPolicy;

        parseNestedPolicy(nestedPolicy, this);
    }

    @Override
    public Policy getPolicy() {
        return nestedPolicy;
    }

    @Override
    public QName getName() {
        return getVersion().getSPConstants().getAlgorithmSuite();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof AlgorithmSuite)) {
            return false;
        }

        AlgorithmSuite that = (AlgorithmSuite)object;
        if (c14n != that.c14n || soapNormType != that.soapNormType || strType != that.strType
            || xPathType != that.xPathType) {
            return false;
        }

        if (algorithmSuiteType != null && !algorithmSuiteType.equals(that.algorithmSuiteType)
            || algorithmSuiteType == null && that.algorithmSuiteType != null) {
            return false;
        }
        if (computedKey != null && !computedKey.equals(that.computedKey)
            || computedKey == null && that.computedKey != null) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (c14n != null) {
            result = 31 * result + c14n.hashCode();
        }
        if (soapNormType != null) {
            result = 31 * result + soapNormType.hashCode();
        }
        if (strType != null) {
            result = 31 * result + strType.hashCode();
        }
        if (xPathType != null) {
            result = 31 * result + xPathType.hashCode();
        }
        if (algorithmSuiteType != null) {
            result = 31 * result + algorithmSuiteType.hashCode();
        }
        if (computedKey != null) {
            result = 31 * result + computedKey.hashCode();
        }

        return 31 * result + super.hashCode();
    }

    @Override
    public PolicyComponent normalize() {
        return super.normalize(getPolicy());
    }

    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        super.serialize(writer, getPolicy());
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new AlgorithmSuite(getVersion(), nestedPolicy);
    }

    protected void parseNestedPolicy(Policy nestedPolicy, AlgorithmSuite algorithmSuite) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (Assertion assertion : assertions) {
                String assertionName = assertion.getName().getLocalPart();
                String assertionNamespace = assertion.getName().getNamespaceURI();
                if (!getVersion().getNamespace().equals(assertionNamespace)) {
                    parseCustomAssertion(assertion);
                    continue;
                }
                AlgorithmSuiteType algorithmSuiteType = ALGORITHM_SUITE_TYPES.get(assertionName);
                if (algorithmSuiteType != null) {
                    if (algorithmSuite.getAlgorithmSuiteType() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }

                    // Clone so as not to change the namespace for other AlgorithmSuiteTypes...
                    AlgorithmSuiteType newAlgorithmSuiteType = new AlgorithmSuiteType(algorithmSuiteType);
                    newAlgorithmSuiteType.setNamespace(getVersion().getNamespace());
                    algorithmSuite.setAlgorithmSuiteType(newAlgorithmSuiteType);
                    continue;
                } else {
                    firstInvalidAlgorithmSuite = assertionName;
                }
                C14NType c14NType = C14NType.lookUp(assertionName);
                if (c14NType != null) {
                    algorithmSuite.setC14n(c14NType);
                    continue;
                }
                SOAPNormType soapNormType = SOAPNormType.lookUp(assertionName);
                if (soapNormType != null) {
                    if (algorithmSuite.getSoapNormType() == SOAPNormType.SOAPNormalization10) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    algorithmSuite.setSoapNormType(soapNormType);
                    continue;
                }
                STRType strType = STRType.lookUp(assertionName);
                if (strType != null) {
                    if (algorithmSuite.getStrType() == STRType.STRTransform10) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    algorithmSuite.setStrType(strType);
                    continue;
                }
                XPathType xPathType = XPathType.lookUp(assertionName);
                if (xPathType != null) {
                    if (algorithmSuite.getXPathType() != XPathType.XPathNone) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    algorithmSuite.setXPathType(xPathType);
                    continue;
                }
            }
        }
    }

    protected void parseCustomAssertion(Assertion assertion) {
    }

    public AlgorithmSuiteType getAlgorithmSuiteType() {
        return algorithmSuiteType;
    }

    protected void setAlgorithmSuiteType(AlgorithmSuiteType algorithmSuiteType) {
        this.algorithmSuiteType = algorithmSuiteType;
    }

    public C14NType getC14n() {
        return c14n;
    }

    protected void setC14n(C14NType c14n) {
        this.c14n = c14n;
    }

    public SOAPNormType getSoapNormType() {
        return soapNormType;
    }

    protected void setSoapNormType(SOAPNormType soapNormType) {
        this.soapNormType = soapNormType;
    }

    public STRType getStrType() {
        return strType;
    }

    protected void setStrType(STRType strType) {
        this.strType = strType;
    }

    public XPathType getXPathType() {
        return xPathType;
    }

    protected void setXPathType(XPathType xPathType) {
        this.xPathType = xPathType;
    }

    public String getComputedKey() {
        return computedKey;
    }

    public static Collection<String> getSupportedAlgorithmSuiteNames() {
        return ALGORITHM_SUITE_TYPES.keySet();
    }

    public String getFirstInvalidAlgorithmSuite() {
        return firstInvalidAlgorithmSuite;
    }

}

