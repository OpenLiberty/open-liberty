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

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.utils.EncryptionConstants;

/**
 * Class HKDFParams (HKDF parameter) is used to specify
 * parameters for the HMAC-based Extract-and-Expand Key Derivation Function.
 * @see <A HREF="https://datatracker.ietf.org/doc/html/rfc5869">HMAC-based
 * Extract-and-Expand Key Derivation Function (HKDF)</A>
 */
public class HKDFParams extends KeyDerivationParameters {

    private String hmacHashAlgorithm;
    private byte[] salt;
    private byte[] info;

    /**
     * Constructor HKDFParams with specified digest algorithm.
     *
     * @param keyBitLength the length of the derived key in bits
     * @param hmacHashAlgorithm the HMAC hash algorithm to use for the key derivation
     */
    protected HKDFParams(int keyBitLength, String hmacHashAlgorithm) {
        super(EncryptionConstants.ALGO_ID_KEYDERIVATION_HKDF, keyBitLength);
        this.hmacHashAlgorithm = hmacHashAlgorithm;
    }

    /**
     * Method return the digest algorithm URI. In case of algorithm is not set, the
     * "default" algorithm http://www.w3.org/2001/04/xmldsig-more#hmac-sha256 URI
     * algorithm is returned.
     *
     * @return the hmac algorithm
     */
    public String getHmacHashAlgorithm() {
        return hmacHashAlgorithm == null? XMLSignature.ALGO_ID_MAC_HMAC_SHA256 : hmacHashAlgorithm;
    }

    /**
     * Method set the digest algorithm URI.
     *
     * @param hmacHashAlgorithm the hmac algorithm URI
     */
    public void setHmacHashAlgorithm(String hmacHashAlgorithm) {
        this.hmacHashAlgorithm = hmacHashAlgorithm;
    }

    /**
     * Method return the salt value which is used for the key derivation.
     *
     * @return the salt value
     */
    public byte[] getSalt() {
        return salt;
    }

    /**
     * Method set the salt value which is used for the key derivation.
     *
     * @param salt
     */
    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    /**
     * Method return the info value which is used for the key derivation.
     *
     * @return the info value
     */
    public byte[] getInfo() {
        return info;
    }

    /**
     * Method set the info value which is used for the key derivation.
     *
     * @param info
     */
    public void setInfo(byte[] info) {
        this.info = info;
    }

    /**
     * Method create a new builder for the HKDFParams.
     * @param keyBitLength the length of the derived key in bits
     * @param hmacHashAlgorithm the HMAC hash algorithm URI to use for the key derivation
     * @return a new builder instance
     */
    public static Builder createBuilder(int keyBitLength, String hmacHashAlgorithm) {
        return new Builder(keyBitLength, hmacHashAlgorithm);
    }

    /**
     * This class is used to create HKDF configuration parameters {@link HKDFParams}.
     * The key length and HMAC hash algorithm are required parameters.
     */
    public static class Builder {
        private final int keyBitLength;
        private final String hmacHashAlgorithm;
        private byte[] salt;
        private byte[] info;

        /**
         * Constructor for the HKDFParams builder. The key bit length and HMAC hash
         * algorithm are required parameters.
         *
         * @param keyBitLength the length of the derived key in bits
         * @param hmacHashAlgorithm the HMAC hash algorithm URI to use for the key derivation
         */
        protected Builder(int keyBitLength, String hmacHashAlgorithm) {
            this.keyBitLength = keyBitLength;
            this.hmacHashAlgorithm = hmacHashAlgorithm;
        }

        /**
         * Set the (optional) salt value which is used for the key derivation.
         * @param salt the hkdf salt value
         * @return the self updated builder instance
         */
        public Builder salt(byte[] salt) {
            this.salt = salt;
            return this;
        }

        /**
         * Set the (optional) info value which is used for the key derivation.
         * @param info the hkdf info value
         * @return the self updated builder instance
         */
        public Builder info(byte[] info) {
            this.info = info;
            return this;
        }

        /**
         * Build the HKDFParams instance with the configured parameters.
         * @return the configured HKDFParams instance
         */
        public HKDFParams build() {
            HKDFParams params = new HKDFParams(keyBitLength, hmacHashAlgorithm);
            params.setSalt(salt);
            params.setInfo(info);
            return params;
        }
    }
}
