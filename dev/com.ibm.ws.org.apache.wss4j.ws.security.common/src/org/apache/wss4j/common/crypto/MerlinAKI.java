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

package org.apache.wss4j.common.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * A Crypto implementation based on two Java KeyStore objects, one being the keystore, and one
 * being the truststore. It differs from Merlin in that it searches the truststore for the
 * issuing cert using the AuthorityKeyIdentifier bytes of the certificate, as opposed to the
 * issuer DN.
 */
public class MerlinAKI extends Merlin {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(MerlinAKI.class);

    public MerlinAKI() {
        super();
    }

    public MerlinAKI(boolean loadCACerts, String cacertsPasswd) {
        super(loadCACerts, cacertsPasswd);
    }

    public MerlinAKI(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
        throws WSSecurityException, IOException {
        super(properties, loader, passwordEncryptor);
    }

    /**
     * Evaluate whether a given certificate chain should be trusted.
     *
     * @param certs Certificate chain to validate
     * @param enableRevocation whether to enable CRL verification or not
     * @param subjectCertConstraints A set of constraints on the Subject DN of the certificates
     *
     * @throws WSSecurityException if the certificate chain is invalid
     */
    @Override
    protected void verifyTrust(
        X509Certificate[] certs,
        boolean enableRevocation,
        Collection<Pattern> subjectCertConstraints
    ) throws WSSecurityException {
        //
        // FIRST step - Search the keystore for the transmitted certificate
        //
        if (certs.length == 1 && !enableRevocation) {
            String issuerString = certs[0].getIssuerX500Principal().getName();
            BigInteger issuerSerial = certs[0].getSerialNumber();

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(issuerString, issuerSerial);
            X509Certificate[] foundCerts = getX509Certificates(cryptoType);

            //
            // If a certificate has been found, the certificates must be compared
            // to ensure against phony DNs (compare encoded form including signature)
            //
            if (foundCerts != null && foundCerts[0] != null && foundCerts[0].equals(certs[0])) {
                try {
                    certs[0].checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILED_CHECK, e, "invalidCert"
                    );
                }
                LOG.debug(
                    "Direct trust for certificate with {}", certs[0].getSubjectX500Principal().getName()
                );
                return;
            }
        }

        //
        // SECOND step - Search for the issuer cert (chain) of the transmitted certificate in the
        // keystore or the truststore
        //
        X509Certificate[] x509certs = certs;
        String issuerString = certs[0].getIssuerX500Principal().getName();
        try {
            if (certs.length == 1) {
                byte[] keyIdentifierBytes =
                    BouncyCastleUtils.getAuthorityKeyIdentifierBytes(certs[0]);
                X509Certificate[] foundCerts = getX509CertificatesFromKeyIdentifier(keyIdentifierBytes);

                // If the certs have not been found, the issuer is not in the keystore/truststore
                // As a direct result, do not trust the transmitted certificate
                if (foundCerts == null || foundCerts.length < 1) {
                    String subjectString = certs[0].getSubjectX500Principal().getName();
                    LOG.debug(
                        "No certs found in keystore for issuer {} of certificate for {}",
                         issuerString, subjectString
                    );
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE, "certpath", new Object[] {"No trusted certs found"}
                    );
                }

                //
                // Form a certificate chain from the transmitted certificate
                // and the certificate(s) of the issuer from the keystore/truststore
                //
                x509certs = new X509Certificate[foundCerts.length + 1];
                x509certs[0] = certs[0];
                System.arraycopy(foundCerts, 0, x509certs, 1, foundCerts.length);
            }
        } catch (NoSuchAlgorithmException | CertificateException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "certpath");
        }

        //
        // THIRD step
        // Check the certificate trust path for the issuer cert chain
        //
        LOG.debug(
            "Preparing to validate certificate path for issuer {}", issuerString
        );

        try {
            // Generate cert path
            List<X509Certificate> certList = Arrays.asList(x509certs);
            CertPath path = getCertificateFactory().generateCertPath(certList);

            Set<TrustAnchor> set = new HashSet<>();
            if (truststore != null) {
                addTrustAnchors(set, truststore);
            }

            //
            // Add certificates from the keystore - only if there is no TrustStore, apart from
            // the case that the truststore is the JDK CA certs. This behaviour is preserved
            // for backwards compatibility reasons
            //
            if (keystore != null && (truststore == null || loadCACerts)) {
                addTrustAnchors(set, keystore);
            }

            // Verify the trust path using the above settings
            String provider = getCryptoProvider();
            CertPathValidator validator = null;
            if (provider == null || provider.length() == 0) {
                validator = CertPathValidator.getInstance("PKIX");
            } else {
                validator = CertPathValidator.getInstance("PKIX", provider);
            }

            PKIXParameters param = createPKIXParameters(set, enableRevocation);
            validator.validate(path, param);
        } catch (NoSuchProviderException | NoSuchAlgorithmException
            | CertificateException | InvalidAlgorithmParameterException
            | java.security.cert.CertPathValidatorException
            | KeyStoreException e) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, e, "certpath"
                );
        }

        // Finally check Cert Constraints
        if (!matchesSubjectDnPattern(certs[0], subjectCertConstraints)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    private X509Certificate[] getX509CertificatesFromKeyIdentifier(
        byte[] keyIdentifierBytes
    ) throws WSSecurityException, NoSuchAlgorithmException, CertificateEncodingException {
        if (keyIdentifierBytes == null) {
            return null;
        }

        Certificate[] certs = null;
        if (keystore != null) {
            certs = getCertificates(keyIdentifierBytes, keystore);
        }

        //If we can't find the issuer in the keystore then look at the truststore
        if ((certs == null || certs.length == 0) && truststore != null) {
            certs = getCertificates(keyIdentifierBytes, truststore);
        }

        if (certs == null || certs.length == 0) {
            return null;
        }

        return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
    }

    private Certificate[] getCertificates(
        byte[] keyIdentifier,
        KeyStore store
    ) throws WSSecurityException, NoSuchAlgorithmException, CertificateEncodingException {
        try {
            for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                Certificate[] certs = store.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate cert = store.getCertificate(alias);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }

                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                    byte[] subjectKeyIdentifier =
                        BouncyCastleUtils.getSubjectKeyIdentifierBytes((X509Certificate)certs[0]);
                    if (subjectKeyIdentifier != null
                        && Arrays.equals(subjectKeyIdentifier, keyIdentifier)) {
                        return certs;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }
        return new Certificate[]{};
    }

}
