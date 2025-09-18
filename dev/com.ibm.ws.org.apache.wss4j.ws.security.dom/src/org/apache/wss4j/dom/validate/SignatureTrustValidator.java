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

package org.apache.wss4j.dom.validate;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;

/**
 * This class verifies trust in a credential used to verify a signature, which is extracted
 * from the Credential passed to the validate method.
 */
public class SignatureTrustValidator implements Validator {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SignatureTrustValidator.class);

    /**
     * Validate the credential argument. It must contain a non-null X509Certificate chain
     * or a PublicKey. A Crypto implementation is also required to be set.
     *
     * This implementation first attempts to verify trust on the certificate (chain). If
     * this is not successful, then it will attempt to verify trust on the Public Key.
     *
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
        }
        X509Certificate[] certs = credential.getCertificates();
        PublicKey publicKey = credential.getPublicKey();
        Crypto crypto = getCrypto(data);
        if (crypto == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noSigCryptoFile");
        }

        if (certs != null && certs.length > 0) {
            validateCertificates(certs);
            verifyTrustInCerts(certs, crypto, data, data.isRevocationEnabled());
            return credential;
        }
        if (publicKey != null) {
            validatePublicKey(publicKey, crypto);
            return credential;
        }
        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
    }


    protected Crypto getCrypto(RequestData data) {
        return data.getSigVerCrypto();
    }

    /**
     * Validate the certificates by checking the validity of each cert
     * @throws WSSecurityException
     */
    protected void validateCertificates(X509Certificate[] certificates)
        throws WSSecurityException {
        // Nothing
    }

    /**
     * Evaluate whether the given certificate chain should be trusted.
     *
     * @param certificates the certificate chain that should be validated against the keystore
     * @param crypto A Crypto instance
     * @param data A RequestData instance
     * @param enableRevocation Whether revocation is enabled or not
     * @throws WSSecurityException if the certificate chain is not trusted
     */
    protected void verifyTrustInCerts(
        X509Certificate[] certificates,
        Crypto crypto,
        RequestData data,
        boolean enableRevocation
    ) throws WSSecurityException {
        //
        // Use the validation method from the crypto to check whether the subjects'
        // certificate was really signed by the issuer stated in the certificate
        //
        Collection<Pattern> subjectCertConstraints = data.getSubjectCertConstraints();
        Collection<Pattern> issuerCertConstraints = data.getIssuerDNPatterns();
        crypto.verifyTrust(certificates, enableRevocation, subjectCertConstraints, issuerCertConstraints);
        String subjectString = certificates[0].getSubjectX500Principal().getName();
        LOG.debug(
            "Certificate path has been verified for certificate with subject {}", subjectString
        );
    }

    /**
     * Validate a public key
     * @throws WSSecurityException
     */
    protected void validatePublicKey(PublicKey publicKey, Crypto crypto)
        throws WSSecurityException {
        crypto.verifyTrust(publicKey);
    }

}
