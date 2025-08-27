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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.x500.X500Principal;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * A Crypto implementation based on a simple array of X509Certificate(s). PrivateKeys are not
 * supported, so this cannot be used for signature creation, or decryption.
 */
public class CertificateStore extends CryptoBase {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(CertificateStore.class);

    private X509Certificate[] trustedCerts;

    /**
     * Constructor
     */
    public CertificateStore(X509Certificate[] trustedCerts) {
        this.trustedCerts = trustedCerts;
    }

    /**
     * Get an X509Certificate (chain) corresponding to the CryptoType argument. The supported
     * types are as follows:
     *
     * TYPE.ISSUER_SERIAL - A certificate (chain) is located by the issuer name and serial number
     * TYPE.THUMBPRINT_SHA1 - A certificate (chain) is located by the SHA1 of the (root) cert
     * TYPE.SKI_BYTES - A certificate (chain) is located by the SKI bytes of the (root) cert
     * TYPE.SUBJECT_DN - A certificate (chain) is located by the Subject DN of the (root) cert
     * Note that TYPE.ALIAS is not allowed, as it doesn't have any meaning with a CertificateStore
     */
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (cryptoType == null) {
            return new X509Certificate[0];
        }
        CryptoType.TYPE type = cryptoType.getType();
        X509Certificate[] certs = new X509Certificate[0];
        switch (type) {
        case ISSUER_SERIAL:
            certs = getX509Certificates(cryptoType.getIssuer(), cryptoType.getSerial());
            break;
        case THUMBPRINT_SHA1:
            certs = getX509Certificates(cryptoType.getBytes());
            break;
        case SKI_BYTES:
            certs = getX509CertificatesSKI(cryptoType.getBytes());
            break;
        case SUBJECT_DN:
            certs = getX509CertificatesSubjectDN(cryptoType.getSubjectDN());
            break;
        case ALIAS:
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "generic.EmptyMessage",
                    new Object[] {"The alias CryptoType is not allowed for CertificateStore"}
            );
        case ENDPOINT:
            break;
        }
        return certs;
    }

    /**
     * Get the implementation-specific identifier corresponding to the cert parameter. In this
     * case, the identifier refers to the subject DN.
     * @param cert The X509Certificate for which to search for an identifier
     * @return the identifier corresponding to the cert parameter
     * @throws WSSecurityException
     */
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        return cert.getSubjectX500Principal().toString();
    }

    /**
     * Gets the private key corresponding to the certificate. Not supported.
     *
     * @param certificate The X509Certificate corresponding to the private key
     * @param callbackHandler The callbackHandler needed to get the password
     * @return The private key
     */
    public PrivateKey getPrivateKey(
        X509Certificate certificate, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        return null;
    }

    /**
     * Gets the private key corresponding to the given PublicKey.
     *
     * @param publicKey The PublicKey corresponding to the private key
     * @param callbackHandler The callbackHandler needed to get the password
     * @return The private key
     */
    public PrivateKey getPrivateKey(
        PublicKey publicKey,
        CallbackHandler callbackHandler
    ) throws WSSecurityException {
        return null;
    }

    /**
     * Gets the private key corresponding to the identifier. Not supported.
     *
     * @param identifier The implementation-specific identifier corresponding to the key
     * @param password The password needed to get the key
     * @return The private key
     */
    public PrivateKey getPrivateKey(
        String identifier,
        String password
    ) throws WSSecurityException {
        return null;
    }

    /**
     * Evaluate whether a given certificate chain should be trusted.
     *
     * @param certs Certificate chain to validate
     * @param enableRevocation whether to enable CRL verification or not
     * @param subjectCertConstraints A set of constraints on the Subject DN of the certificates
     * @throws WSSecurityException if the certificate chain is invalid
     */
    protected void verifyTrust(
        X509Certificate[] certs,
        boolean enableRevocation,
        Collection<Pattern> subjectCertConstraints
    ) throws WSSecurityException {
        //
        // FIRST step - Search the trusted certs for the transmitted certificate
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
            if (foundCerts != null && foundCerts.length > 0 && foundCerts[0] != null && foundCerts[0].equals(certs[0])) {
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
        String issuerString = certs[0].getIssuerX500Principal().getName();
        X509Certificate[] foundCerts = new X509Certificate[0];
        if (certs.length == 1) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SUBJECT_DN);
            cryptoType.setSubjectDN(issuerString);
            foundCerts = getX509Certificates(cryptoType);

            // If the certs have not been found, the issuer is not in the keystore/truststore
            // As a direct result, do not trust the transmitted certificate
            if (foundCerts == null || foundCerts.length < 1) {
                String subjectString = certs[0].getSubjectX500Principal().getName();
                LOG.debug(
                    "No certs found in keystore for issuer {} of certificate for {}", issuerString, subjectString
                );
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "certpath",
                    new Object[] {"No trusted certs found"}
                );
            }
        }

        //
        // THIRD step
        // Check the certificate trust path for the issuer cert chain
        //
        LOG.debug(
            "Preparing to validate certificate path for issuer {}", issuerString
        );

        try {
            Set<TrustAnchor> set = new HashSet<>();
            if (trustedCerts != null) {
                for (X509Certificate cert : trustedCerts) {
                    TrustAnchor anchor =
                        new TrustAnchor(cert, null);
                    set.add(anchor);
                }
            }

            // Verify the trust path using the above settings
            String provider = getCryptoProvider();
            CertPathValidator validator = null;
            if (provider == null || provider.length() == 0) {
                validator = CertPathValidator.getInstance("PKIX");
            } else {
                validator = CertPathValidator.getInstance("PKIX", provider);
            }

            PKIXParameters param = new PKIXParameters(set);
            param.setRevocationEnabled(enableRevocation);

            if (foundCerts.length > 0) {
                //
                // Form a certificate chain from the transmitted certificate
                // and the certificate(s) of the issuer from the keystore/truststore
                //
                X509Certificate[] x509certs = new X509Certificate[foundCerts.length + 1];
                x509certs[0] = certs[0];
                System.arraycopy(foundCerts, 0, x509certs, 1, foundCerts.length);

                // Generate cert path
                List<X509Certificate> certList = Arrays.asList(x509certs);
                CertPath path = getCertificateFactory().generateCertPath(certList);

                validator.validate(path, param);
            } else {
                List<X509Certificate> certList = Arrays.asList(certs);
                CertPath path = getCertificateFactory().generateCertPath(certList);

                validator.validate(path, param);
            }
        } catch (java.security.NoSuchProviderException | NoSuchAlgorithmException
            | java.security.cert.CertificateException
            | java.security.InvalidAlgorithmParameterException
            | java.security.cert.CertPathValidatorException e) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, e, "certpath",
                    new Object[] {e.getMessage()}
                );
        }

        // Finally check Cert Constraints
        if (!matchesSubjectDnPattern(certs[0], subjectCertConstraints)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    @Override
    public void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints,
                            Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        verifyTrust(certs, enableRevocation, subjectCertConstraints);
        if (!matchesIssuerDnPattern(certs[0], issuerCertConstraints)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    /**
     * Evaluate whether a given public key should be trusted.
     *
     * @param publicKey The PublicKey to be evaluated
     * @throws WSSecurityException if the PublicKey is invalid
     */
    public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
        //
        // If the public key is null, do not trust the signature
        //
        if (publicKey == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        //
        // Search the trusted certs for the transmitted public key (direct trust)
        //
        for (X509Certificate trustedCert : trustedCerts) {
            if (publicKey.equals(trustedCert.getPublicKey())) {
                return;
            }
        }
        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
    }

    /**
     * Get an X509 Certificate (chain) according to a given serial number and issuer string.
     *
     * @param issuer The Issuer String
     * @param serialNumber The serial number of the certificate
     * @return the X509 Certificate (chain) that was found
     * @throws WSSecurityException
     */
    private X509Certificate[] getX509Certificates(
        String issuer,
        BigInteger serialNumber
    ) throws WSSecurityException {
        //
        // Convert the subject DN to a java X500Principal object first. This is to ensure
        // interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
        // Then convert it to a BouncyCastle X509Name, which will order the attributes of
        // the DN in a particular way (see WSS-168). If the conversion to an X500Principal
        // object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
        // back on a direct conversion to a BC X509Name
        //
        Object issuerName = null;
        try {
            X500Principal issuerRDN = new X500Principal(issuer);
            issuerName = createBCX509Name(issuerRDN.getName());
        } catch (IllegalArgumentException ex) {
            issuerName = createBCX509Name(issuer);
        }

        for (X509Certificate trustedCert : trustedCerts) {
            if (trustedCert.getSerialNumber().compareTo(serialNumber) == 0) {
                Object certName =
                    createBCX509Name(trustedCert.getIssuerX500Principal().getName());
                if (certName.equals(issuerName)) {
                    return new X509Certificate[]{trustedCert};
                }
            }
        }

        return new X509Certificate[0];
    }

    /**
     * Get an X509 Certificate (chain) according to a given Thumbprint.
     *
     * @param thumb The SHA1 thumbprint info bytes
     * @return the X509 certificate (chain) that was found (can be null)
     * @throws WSSecurityException if problems during keystore handling or wrong certificate
     */
    private X509Certificate[] getX509Certificates(byte[] thumb) throws WSSecurityException {
        MessageDigest sha = null;

        if (trustedCerts == null) {
            return new X509Certificate[0];
        }

        try {
            sha = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "decoding.general"
            );
        }
        for (X509Certificate trustedCert : trustedCerts) {
            try {
                sha.update(trustedCert.getEncoded());
            } catch (CertificateEncodingException ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, ex, "encodeError"
                );
            }
            byte[] data = sha.digest();

            if (Arrays.equals(data, thumb)) {
                return new X509Certificate[]{trustedCert};
            }
        }
        return new X509Certificate[0];
    }

    /**
     * Get an X509 Certificate (chain) according to a given SubjectKeyIdentifier.
     *
     * @param skiBytes The SKI bytes
     * @return the X509 certificate (chain) that was found (can be null)
     */
    private X509Certificate[] getX509CertificatesSKI(byte[] skiBytes) throws WSSecurityException {
        if (trustedCerts == null) {
            return new X509Certificate[0];
        }
        for (X509Certificate trustedCert : trustedCerts) {
            byte[] data = getSKIBytesFromCert(trustedCert);
            if (data.length == skiBytes.length && Arrays.equals(data, skiBytes)) {
                return new X509Certificate[]{trustedCert};
            }
        }
        return new X509Certificate[0];
    }

    /**
     * Get an X509 Certificate (chain) according to a given DN of the subject of the certificate
     *
     * @param subjectDN The DN of subject to look for
     * @return An X509 Certificate (chain) with the same DN as given in the parameters
     * @throws WSSecurityException
     */
    private X509Certificate[] getX509CertificatesSubjectDN(String subjectDN)
        throws WSSecurityException {
        //
        // Convert the subject DN to a java X500Principal object first. This is to ensure
        // interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
        // Then convert it to a BouncyCastle X509Name, which will order the attributes of
        // the DN in a particular way (see WSS-168). If the conversion to an X500Principal
        // object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
        // back on a direct conversion to a BC X509Name
        //
        Object subject;
        try {
            X500Principal subjectRDN = new X500Principal(subjectDN);
            subject = createBCX509Name(subjectRDN.getName());
        } catch (IllegalArgumentException ex) {
            subject = createBCX509Name(subjectDN);
        }

        if (trustedCerts != null) {
            for (X509Certificate trustedCert : trustedCerts) {
                X500Principal foundRDN = trustedCert.getSubjectX500Principal();
                Object certName = createBCX509Name(foundRDN.getName());

                if (subject.equals(certName)) {
                    return new X509Certificate[]{trustedCert};
                }
            }
        }

        return new X509Certificate[0];
    }

}
