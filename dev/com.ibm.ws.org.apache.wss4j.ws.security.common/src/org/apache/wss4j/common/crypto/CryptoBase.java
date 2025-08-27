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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * This Abstract Base Class implements the accessor and keystore-independent methods and
 * functionality of the Crypto interface.
 */
public abstract class CryptoBase implements Crypto {
    public static final String SKI_OID = "2.5.29.14";  //NOPMD - not an IP address
    /**
     * OID For the NameConstraints Extension to X.509
     *
     * http://java.sun.com/j2se/1.4.2/docs/api/
     * http://www.ietf.org/rfc/rfc3280.txt (s. 4.2.1.11)
     */
    public static final String NAME_CONSTRAINTS_OID = "2.5.29.30";  //NOPMD - not an IP address

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(CryptoBase.class);

    private static final Constructor<?> BC_509CLASS_CONS;

    protected CertificateFactory certificateFactory;
    private String defaultAlias;
    private String cryptoProvider;
    private String trustProvider;

    static {
        Constructor<?> cons = null;
        try {
            Class<?> c = Class.forName("org.bouncycastle.asn1.x500.X500Name");
            cons = c.getConstructor(new Class[] {String.class});
        } catch (Exception e) { //NOPMD
          //ignore
        }
        BC_509CLASS_CONS = cons;
    }

    /**
     * Constructor
     */
    protected CryptoBase() {
    }

    /**
     * Get the crypto provider associated with this implementation
     * @return the crypto provider
     */
    public String getCryptoProvider() {
        return cryptoProvider;
    }

    /**
     * Set the crypto provider associated with this implementation
     * @param provider the crypto provider to set
     */
    public void setCryptoProvider(String provider) {
        cryptoProvider = provider;
    }

    /**
     * Set the crypto provider used for truststore operations associated with this implementation
     * @param provider the name of the provider
     */
    public void setTrustProvider(String provider) {
        trustProvider = provider;
    }

    /**
     * Get the crypto provider used for truststore operation associated with this implementation.
     * @return a crypto provider name
     */
    public String getTrustProvider() {
        return trustProvider;
    }

    /**
     * Retrieves the identifier name of the default certificate. This should be the certificate
     * that is used for signature and encryption. This identifier corresponds to the certificate
     * that should be used whenever KeyInfo is not present in a signed or an encrypted
     * message. May return null. The identifier is implementation specific, e.g. it could be the
     * KeyStore alias.
     *
     * @return name of the default X509 certificate.
     */
    public String getDefaultX509Identifier() throws WSSecurityException {
        return defaultAlias;
    }

    /**
     * Sets the identifier name of the default certificate. This should be the certificate
     * that is used for signature and encryption. This identifier corresponds to the certificate
     * that should be used whenever KeyInfo is not present in a signed or an encrypted
     * message. The identifier is implementation specific, e.g. it could be the KeyStore alias.
     *
     * @param identifier name of the default X509 certificate.
     */
    public void setDefaultX509Identifier(String identifier) {
        defaultAlias = identifier;
    }

    /**
     * Sets the CertificateFactory instance on this Crypto instance
     *
     * @param certFactory the CertificateFactory the CertificateFactory instance to set
     */
    public void setCertificateFactory(CertificateFactory certFactory) {
        this.certificateFactory = certFactory;
    }

    /**
     * Get the CertificateFactory instance on this Crypto instance
     *
     * @return Returns a <code>CertificateFactory</code> to construct
     *         X509 certificates
     * @throws WSSecurityException
     */
    public CertificateFactory getCertificateFactory() throws WSSecurityException {
        if (certificateFactory != null) {
            return certificateFactory;
        }

        try {
            String provider = getCryptoProvider();
            if (provider == null || provider.length() == 0) {
                certificateFactory = CertificateFactory.getInstance("X.509");
            } else {
                certificateFactory = CertificateFactory.getInstance("X.509", provider);
            }
        } catch (CertificateException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "unsupportedCertType"
            );
        } catch (NoSuchProviderException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "noSecProvider"
            );
        }

        return certificateFactory;
    }

    /**
     * Load a X509Certificate from the input stream.
     *
     * @param in The <code>InputStream</code> containing the X509Certificate
     * @return An X509 certificate
     * @throws WSSecurityException
     */
    public X509Certificate loadCertificate(InputStream in) throws WSSecurityException {
        try {
            CertificateFactory certFactory = getCertificateFactory();
            return (X509Certificate) certFactory.generateCertificate(in);
        } catch (CertificateException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError"
            );
        }
    }

    /**
     * Reads the SubjectKeyIdentifier information from the certificate.
     * <p/>
     * If the the certificate does not contain a SKI extension then
     * try to compute the SKI according to RFC3280 using the
     * SHA-1 hash value of the public key. The second method described
     * in RFC3280 is not support. Also only RSA public keys are supported.
     * If we cannot compute the SKI throw a WSSecurityException.
     *
     * @param cert The certificate to read SKI
     * @return The byte array containing the binary SKI data
     */
    public byte[] getSKIBytesFromCert(X509Certificate cert) throws WSSecurityException {
        //
        // Gets the DER-encoded OCTET string for the extension value (extnValue)
        // identified by the passed-in oid String. The oid string is represented
        // by a set of positive whole numbers separated by periods.
        //
        byte[] derEncodedValue = cert.getExtensionValue(SKI_OID);

        if (cert.getVersion() < 3 || derEncodedValue == null) {
            X509SubjectPublicKeyInfo spki = new X509SubjectPublicKeyInfo(cert.getPublicKey());
            byte[] value = spki.getSubjectPublicKey();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                return digest.digest(value);
            } catch (Exception ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN, ex, "noSKIHandling",
                    new Object[] {"No SKI certificate extension and no SHA1 message digest available"}
                );
            }
        }

        //
        // Strip away first (four) bytes from the DerValue (tag and length of
        // ExtensionValue OCTET STRING and KeyIdentifier OCTET STRING)
        //
        DERDecoder extVal = new DERDecoder(derEncodedValue);
        extVal.expect(DERDecoder.TYPE_OCTET_STRING);  // ExtensionValue OCTET STRING
        extVal.getLength();
        extVal.expect(DERDecoder.TYPE_OCTET_STRING);  // KeyIdentifier OCTET STRING
        int keyIDLen = extVal.getLength();
        return extVal.getBytes(keyIDLen);
    }

    /**
     * Get a byte array given an array of X509 certificates.
     * <p/>
     *
     * @param certs The certificates to convert
     * @return The byte array for the certificates
     * @throws WSSecurityException
     */
    public byte[] getBytesFromCertificates(X509Certificate[] certs)
        throws WSSecurityException {
        try {
            CertPath path = getCertificateFactory().generateCertPath(Arrays.asList(certs));
            return path.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "encodeError"
            );
        } catch (CertificateException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError"
            );
        }
    }

    /**
     * Construct an array of X509Certificate's from the byte array.
     * <p/>
     *
     * @param data The <code>byte</code> array containing the X509 data
     * @return An array of X509 certificates
     * @throws WSSecurityException
     */
    public X509Certificate[] getCertificatesFromBytes(byte[] data)
        throws WSSecurityException {
        CertPath path = null;
        try (InputStream in = new ByteArrayInputStream(data)) {
            path = getCertificateFactory().generateCertPath(in);
        } catch (CertificateException | IOException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError"
            );
        }

        List<?> l = path.getCertificates();
        X509Certificate[] certs = new X509Certificate[l.size()];
        int i = 0;
        for (Object cert : l) {
            certs[i++] = (X509Certificate) cert;
        }
        return certs;
    }

    protected Object createBCX509Name(String s) {
        if (BC_509CLASS_CONS != null) {
             try {
                 return BC_509CLASS_CONS.newInstance(s);
             } catch (Exception e) { //NOPMD
                 //ignore
             }
        }
        return new X500Principal(s);
    }

    /**
     * @return      true if the certificate's SubjectDN matches the constraints defined in the
     *              subject DNConstraints; false, otherwise. The certificate subject DN only
     *              has to match ONE of the subject cert constraints (not all).
     */
    protected boolean
    matchesSubjectDnPattern(
        final X509Certificate cert, final Collection<Pattern> subjectDNPatterns
    ) {
        if (cert == null) {
            LOG.debug("The certificate is null so no constraints matching was possible");
            return false;
        }
        String subjectName = cert.getSubjectX500Principal().getName();
        if (subjectDNPatterns == null || subjectDNPatterns.isEmpty()) {
            LOG.warn("No Subject DN Certificate Constraints were defined. This could be a security issue");
            return true;
        }
        return matchesName(subjectName, subjectDNPatterns);
    }

    /**
     * @return      true if the certificate's Issuer DN matches the constraints defined in the
     *              subject DNConstraints; false, otherwise. The certificate subject DN only
     *              has to match ONE of the subject cert constraints (not all).
     */
    protected boolean
    matchesIssuerDnPattern(
        final X509Certificate cert, final Collection<Pattern> issuerDNPatterns
    ) {
        if (cert == null) {
            LOG.debug("The certificate is null so no constraints matching was possible");
            return false;
        }
        String issuerDn = cert.getIssuerX500Principal().getName();
        return matchesName(issuerDn, issuerDNPatterns);
    }

    /**
     * @return      true if the provided name matches the constraints defined in the
     *              subject DNConstraints; false, otherwise. The certificate (subject) DN only
     *              has to match ONE of the (subject) cert constraints (not all).
     */
    protected boolean
    matchesName(
        final String name, final Collection<Pattern> patterns
    ) {
        if (patterns != null && !patterns.isEmpty()) {
            if (name == null || name.isEmpty()) {
                LOG.debug("The name is null so no constraints matching was possible");
                return false;
            }
            boolean subjectMatch = false;
            for (Pattern subjectDNPattern : patterns) {
                final Matcher matcher = subjectDNPattern.matcher(name);
                if (matcher.matches()) {
                    LOG.debug("Name {} matches with pattern {}", name, subjectDNPattern);
                    subjectMatch = true;
                    break;
                }
            }
            if (!subjectMatch) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts the NameConstraints sequence from the certificate.
     * Handles the case where the data is encoded directly as {@link DERDecoder#TYPE_SEQUENCE}
     * or where the sequence has been encoded as an {@link DERDecoder#TYPE_OCTET_STRING}.
     * <p>
     * By contract, the values retrieved from calls to {@link X509Certificate#getExtensionValue(String)}
     * should always be DER-encoded OCTET strings; however, because of ambiguity in the RFC and
     * the potential for a future breaking change to this contract, testing whether the bytes
     * returned are tagged as a sequence or an encoded octet string is prudent. Considering the fact
     * that it is a single byte comparison, the performance hit is negligible.
     *
     * @param cert the certificate to extract NameConstraints from
     * @return the NameConstraints, or null if not present
     * @throws WSSecurityException if a processing error occurs decoding the Octet String
     */
    protected byte[] getNameConstraints(final X509Certificate cert) throws WSSecurityException {
        byte[] bytes = cert.getExtensionValue(NAME_CONSTRAINTS_OID);
        if (bytes == null || bytes.length <= 0) {
            return new byte[0];
        }

        switch (bytes[0]) {
            case DERDecoder.TYPE_OCTET_STRING:
                DERDecoder extVal = new DERDecoder(bytes);
                extVal.expect(DERDecoder.TYPE_OCTET_STRING);
                int seqLen = extVal.getLength();
                return extVal.getBytes(seqLen);
            case DERDecoder.TYPE_SEQUENCE:
                return bytes;
            default:
                throw new IllegalArgumentException(
                        "Invalid type for NameConstraints; must be Sequence or OctetString-encoded Sequence");
        }
    }
}
