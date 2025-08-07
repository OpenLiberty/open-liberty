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

package org.apache.wss4j.dom.processor;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.str.STRParser;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Class contains the result of locating public key using the KeyInfoType element.
 * The result is either a PublicKey or/and an X509Certificate chain with the STRParser.REFERENCE_TYPE.
 */
public class CertificateResult {

    /**
     * CertificateResult builder class.
     */
    static final class Builder {
        private X509Certificate[] certs;
        private PublicKey publicKey;
        private STRParser.REFERENCE_TYPE referenceType;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder certificates(X509Certificate[] certs) {
            this.certs = certs;
            return this;
        }

        public Builder publicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder certificatesReferenceType(STRParser.REFERENCE_TYPE referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        /**
         * Method to build the CertificateResult object.
         *
         * @return the CertificateResult object
         * @throws WSSecurityException if the result is empty.
         */
        public CertificateResult build() throws WSSecurityException {
            if (publicKey == null && (certs == null || certs.length < 1 || certs[0] == null)) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE,
                        "noCertsFound",
                        new Object[] {"decryption (KeyId)"});
            }
            if (certs != null && certs.length > 0) {
                publicKey = certs[0].getPublicKey();
            }

            return new CertificateResult(certs, publicKey, referenceType);
        }
    }

    private final X509Certificate[] certs;
    private final PublicKey publicKey;
    private final STRParser.REFERENCE_TYPE referenceType;

    protected CertificateResult(X509Certificate[] certs, PublicKey publicKey, STRParser.REFERENCE_TYPE referenceType) {
        this.certs = certs;
        this.publicKey = publicKey;
        this.referenceType = referenceType;
    }

    public X509Certificate[] getCerts() {
        return certs;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public STRParser.REFERENCE_TYPE getCertificatesReferenceType() {
        return referenceType;
    }
}
