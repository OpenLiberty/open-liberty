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
package org.apache.xml.security.encryption.params;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.utils.EncryptionConstants;

/**
 * Class ConcatKDFParams is used to specify parameters for the
 * ConcatKDF key derivation algorithm.
 *
 * @see <A HREF="https://www.w3.org/TR/xmlenc-core1/#sec-ConcatKDF">XML Encryption
 * syntax and Processing Version 1.1, 5.8.1 The ConcatKDF Key Derivation Algorithm</A>
 */
public class ConcatKDFParams extends KeyDerivationParameters {

    private String digestAlgorithm;
    private String algorithmID;
    private String partyUInfo;
    private String partyVInfo;
    private String suppPubInfo;
    private String suppPrivInfo;

    /**
     * Constructor ConcatKDFParams with specified digest algorithm
     *
     * @param keyBitLength    the length of the derived key in bits
     * @param digestAlgorithm the digest algorithm to use
     */
    protected ConcatKDFParams(int keyBitLength, String digestAlgorithm) {
        super(EncryptionConstants.ALGO_ID_KEYDERIVATION_CONCATKDF, keyBitLength);
        this.digestAlgorithm = digestAlgorithm;
    }

    /**
     * Method return the digest algorithm. In case of algorithm is not set, the "default"
     * algorithm SHA256 digest algorithm is returned.
     *
     * @return the digest algorithm
     */
    public String getDigestAlgorithm() {
        return digestAlgorithm == null ? MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256 : digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getAlgorithmID() {
        return algorithmID;
    }

    public void setAlgorithmID(String algorithmID) {
        this.algorithmID = algorithmID;
    }

    public String getPartyUInfo() {
        return partyUInfo;
    }

    public void setPartyUInfo(String partyUInfo) {
        this.partyUInfo = partyUInfo;
    }

    public String getPartyVInfo() {
        return partyVInfo;
    }

    public void setPartyVInfo(String partyVInfo) {
        this.partyVInfo = partyVInfo;
    }

    public String getSuppPubInfo() {
        return suppPubInfo;
    }

    public void setSuppPubInfo(String suppPubInfo) {
        this.suppPubInfo = suppPubInfo;
    }

    public String getSuppPrivInfo() {
        return suppPrivInfo;
    }

    public void setSuppPrivInfo(String suppPrivInfo) {
        this.suppPrivInfo = suppPrivInfo;
    }

    /**
     * Create a new ConcatKDF key derivation algorithm parameters builder.
     * @param keyBitLength    the length of the derived key in bits
     * @param digestAlgorithm the digest algorithm to use
     * @return a new ConcatKDF builder to configure the key derivation parameters
     */
    public static Builder createBuilder(int keyBitLength, String digestAlgorithm) {
        return new Builder(keyBitLength, digestAlgorithm);
    }

    /**
     * The ConcatKDF key derivation algorithm parameters builder.
     */
    public static class Builder {

        private int keyBitLength;
        private String digestAlgorithm;
        private String algorithmID;
        private String partyUInfo;
        private String partyVInfo;
        private String suppPubInfo;
        private String suppPrivInfo;

        protected Builder(int keyBitLength, String digestAlgorithm) {
            this.keyBitLength = keyBitLength;
            this.digestAlgorithm = digestAlgorithm;
        }

        public Builder algorithmID(String algorithmID) {
            this.algorithmID = algorithmID;
            return this;
        }

        public Builder partyUInfo(String partyUInfo) {
            this.partyUInfo = partyUInfo;
            return this;
        }

        public Builder partyVInfo(String partyVInfo) {
            this.partyVInfo = partyVInfo;
            return this;
        }

        public Builder suppPubInfo(String suppPubInfo) {
            this.suppPubInfo = suppPubInfo;
            return this;
        }

        public Builder suppPrivInfo(String suppPrivInfo) {
            this.suppPrivInfo = suppPrivInfo;
            return this;
        }

        /**
         * Method builds the ConcatKDF key derivation algorithm parameters from
         * the builder configuration.
         */
        public ConcatKDFParams build() {
            ConcatKDFParams params = new ConcatKDFParams(keyBitLength, digestAlgorithm);
            params.setAlgorithmID(algorithmID);
            params.setPartyUInfo(partyUInfo);
            params.setPartyVInfo(partyVInfo);
            params.setSuppPubInfo(suppPubInfo);
            params.setSuppPrivInfo(suppPrivInfo);
            return params;
        }
    }
}
