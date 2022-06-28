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
package org.apache.ws.security.components.crypto;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.security.auth.callback.CallbackHandler;
//import org.apache.ws.security.WSSecurityException;


/**
 * This interface serves as a way to to mitigate the migration from WSS4J 1.6 to 2.0 as required by
 * the migration of CXF from 2.6.2 to 3.4. We modify it to be a interface rather than a class in order 
 * for the 2.0 version, which has a different package name, to implement it. Meaning that users running an application
 * with a custom callback handler based off 1.6 can continue running their
 * applications and have the updated code. 
 */
// Liberty Change: Interface
public interface Crypto {

    //
    // Accessor methods
    //

    /**
     * Get the crypto provider associated with this implementation
     * @return the crypto provider
     */
    String getCryptoProvider();

    /**
     * Set the crypto provider associated with this implementation
     * @param provider the crypto provider name to set
     */
    void setCryptoProvider(String provider);

    /**
     * Get the crypto provider used for truststore operation associated with this implementation.
     * @return a crypto provider name
     */
    String getTrustProvider();

    /**
     * Set the crypto provider used for truststore operations associated with this implementation
     * @param provider the name of the provider
     */
    void setTrustProvider(String provider);

    /**
     * Retrieves the identifier name of the default certificate. This should be the certificate
     * that is used for signature and encryption. This identifier corresponds to the certificate
     * that should be used whenever KeyInfo is not present in a signed or an encrypted
     * message. May return null. The identifier is implementation specific, e.g. it could be the
     * KeyStore alias.
     *
     * @return name of the default X509 certificate.
     */
    String getDefaultX509Identifier() throws Exception;

    /**
     * Sets the identifier name of the default certificate. This should be the certificate
     * that is used for signature and encryption. This identifier corresponds to the certificate
     * that should be used whenever KeyInfo is not present in a signed or an encrypted
     * message. The identifier is implementation specific, e.g. it could be the KeyStore alias.
     *
     * @param identifier name of the default X509 certificate.
     */
    void setDefaultX509Identifier(String identifier);

    /**
     * Sets the CertificateFactory instance on this Crypto instance
     *
     * @param certFactory the CertificateFactory the CertificateFactory instance to set
     */
    void setCertificateFactory(CertificateFactory certFactory);

    /**
     * Get the CertificateFactory instance on this Crypto instance
     *
     * @return Returns a <code>CertificateFactory</code> to construct
     *         X509 certificates
     * @throws WSSecurityException
     */
    CertificateFactory getCertificateFactory() throws Exception;

    //
    // Base Crypto functionality methods
    //

    /**
     * Load a X509Certificate from the input stream.
     *
     * @param in The <code>InputStream</code> containing the X509 data
     * @return An X509 certificate
     * @throws WSSecurityException
     */
    X509Certificate loadCertificate(InputStream in) throws Exception;

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
    byte[] getSKIBytesFromCert(X509Certificate cert) throws Exception;

    /**
     * Get a byte array given an array of X509 certificates.
     * <p/>
     *
     * @param certs The certificates to convert
     * @return The byte array for the certificates
     * @throws WSSecurityException
     */
    byte[] getBytesFromCertificates(X509Certificate[] certs) throws Exception;

    /**
     * Construct an array of X509Certificate's from the byte array.
     *
     * @param data The <code>byte</code> array containing the X509 data
     * @return An array of X509 certificates
     * @throws WSSecurityException
     */
    X509Certificate[] getCertificatesFromBytes(byte[] data) throws Exception;

    //
    // Implementation-specific Crypto functionality methods
    //

    /**
     * Get an X509Certificate (chain) corresponding to the CryptoType argument. The supported
     * types are as follows:
     *
     * TYPE.ISSUER_SERIAL - A certificate (chain) is located by the issuer name and serial number
     * TYPE.THUMBPRINT_SHA1 - A certificate (chain) is located by the SHA1 of the (root) cert
     * TYPE.SKI_BYTES - A certificate (chain) is located by the SKI bytes of the (root) cert
     * TYPE.SUBJECT_DN - A certificate (chain) is located by the Subject DN of the (root) cert
     * TYPE.ALIAS - A certificate (chain) is located by an alias. This alias is implementation
     * specific, for example - it could be a java KeyStore alias.
     
    X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException; */

    /**
     * Get the implementation-specific identifier corresponding to the cert parameter, e.g. the
     * identifier could be a KeyStore alias.
     * @param cert The X509Certificate for which to search for an identifier
     * @return the identifier corresponding to the cert parameter
     * @throws WSSecurityException
     */
    String getX509Identifier(X509Certificate cert) throws Exception;

    /**
     * Gets the private key corresponding to the certificate.
     *
     * @param certificate The X509Certificate corresponding to the private key
     * @param callbackHandler The callbackHandler needed to get the password
     * @return The private key
     */
    PrivateKey getPrivateKey(
        X509Certificate certificate, CallbackHandler callbackHandler
    ) throws Exception;

    /**
     * Gets the private key corresponding to the given PublicKey.
     *
     * @param publicKey The PublicKey corresponding to the private key
     * @param callbackHandler The callbackHandler needed to get the password
     * @return The private key
     */
    PrivateKey getPrivateKey(
        PublicKey publicKey,
        CallbackHandler callbackHandler
    ) throws Exception;

    /**
     * Gets the private key corresponding to the identifier.
     *
     * @param identifier The implementation-specific identifier corresponding to the key
     * @param password The password needed to get the key
     * @return The private key
     */
    PrivateKey getPrivateKey(
        String identifier, String password
    ) throws Exception;

    /**
     * Evaluate whether a given certificate chain should be trusted.
     *
     * @param certs Certificate chain to validate
     * @param enableRevocation whether to enable CRL verification or not
     * @param subjectCertConstraints A set of constraints on the Subject DN of the certificates
     * @param issuerCertConstraints A set of constraints on the Issuer DN of the certificates
     * @throws WSSecurityException if the certificate chain is invalid
     */
    void verifyTrust(
        X509Certificate[] certs, boolean enableRevocation,
        Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints
    ) throws Exception;

    /**
     * Evaluate whether a given public key should be trusted.
     *
     * @param publicKey The PublicKey to be evaluated
     * @throws WSSecurityException if the PublicKey is invalid
     */
    void verifyTrust(PublicKey publicKey) throws Exception;

}
