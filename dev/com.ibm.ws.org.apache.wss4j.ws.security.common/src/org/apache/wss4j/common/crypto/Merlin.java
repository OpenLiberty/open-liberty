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
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import com.ibm.ws.common.crypto.CryptoUtils;
/**
 * A Crypto implementation based on two Java KeyStore objects, one being the keystore, and one
 * being the truststore.
 */
public class Merlin extends CryptoBase {

    public static final String ENCRYPTED_PASSWORD_PREFIX = "ENC(";
    public static final String ENCRYPTED_PASSWORD_SUFFIX = ")";

    public static final String PREFIX = "org.apache.wss4j.crypto.merlin.";
    public static final String OLD_PREFIX = "org.apache.ws.security.crypto.merlin.";

    /*
     * Deprecated types
     */
    public static final String OLD_KEYSTORE_FILE = "file";

    /*
     * Crypto providers
     */
    public static final String CRYPTO_KEYSTORE_PROVIDER = "keystore.provider";
    public static final String CRYPTO_CERT_PROVIDER = "cert.provider";
    public static final String CRYPTO_CERT_PROVIDER_HANDLES_NAME_CONSTRAINTS =
            "cert.provider.nameconstraints";

    /*
     * KeyStore configuration types
     */
    public static final String KEYSTORE_FILE = "keystore.file";
    public static final String KEYSTORE_PASSWORD = "keystore.password";
    public static final String KEYSTORE_TYPE = "keystore.type";
    public static final String KEYSTORE_ALIAS = "keystore.alias";
    public static final String KEYSTORE_PRIVATE_PASSWORD = "keystore.private.password";
    public static final String KEYSTORE_PRIVATE_KEY_CACHING = "keystore.private.caching";

    /*
     * TrustStore configuration types
     */
    public static final String LOAD_CA_CERTS = "load.cacerts";
    public static final String TRUSTSTORE_FILE = "truststore.file";
    public static final String TRUSTSTORE_PASSWORD = "truststore.password";
    public static final String TRUSTSTORE_TYPE = "truststore.type";
    public static final String TRUSTSTORE_PROVIDER = "truststore.provider";

    /*
     * CRL configuration
     */
    public static final String X509_CRL_FILE = "x509crl.file";

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(Merlin.class);
    private static final String COMMA_SEPARATOR = ",";

    protected Properties properties;
    protected KeyStore keystore;
    protected KeyStore truststore;
    protected CertStore crlCertStore;
    protected boolean loadCACerts;
    protected boolean privatePasswordSet;
    protected PasswordEncryptor passwordEncryptor;

    private boolean certProviderHandlesNameConstraints = false;
    private boolean enablePrivateKeyCaching = true;
    private Map<String, PrivateKey> privateKeyCache = new ConcurrentHashMap<>();

    public Merlin() {
        // default constructor
    }

    public Merlin(boolean loadCACerts, String cacertsPasswd) {
        super();

        if (truststore == null && loadCACerts) {
            String cacertsPath = System.getProperty("java.home") + "/lib/security/cacerts";

            try (InputStream cacertsIs = Files.newInputStream(Paths.get(cacertsPath))) {

                truststore = KeyStore.getInstance(KeyStore.getDefaultType());
                truststore.load(cacertsIs, cacertsPasswd.toCharArray());
                this.loadCACerts = true;
            } catch (Exception e) {
                LOG.warn("CA certs could not be loaded: " + e.getMessage());
            }
        }
    }

    public Merlin(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
        throws WSSecurityException, IOException {
        loadProperties(properties, loader, passwordEncryptor);
    }

    public void loadProperties(
        Properties properties,
        ClassLoader loader,
        PasswordEncryptor passwordEncryptor
    ) throws WSSecurityException, IOException {
        if (properties == null) {
            return;
        }
        this.properties = properties;
        this.passwordEncryptor = passwordEncryptor;

        String prefix = PREFIX;
        for (Object key : properties.keySet()) {
            if (key instanceof String) {
                String propKey = (String)key;
                if (propKey.startsWith(PREFIX)) {
                    break;
                } else if (propKey.startsWith(OLD_PREFIX)) {
                    prefix = OLD_PREFIX;
                    break;
                }
            }
        }

        //
        // Load the provider(s)
        //
        String keystoreProvider = this.properties.getProperty(prefix + CRYPTO_KEYSTORE_PROVIDER);
        if (keystoreProvider != null) {
            keystoreProvider = keystoreProvider.trim();

            if (keystoreProvider.length() == 0) {
                keystoreProvider = null;
            }
        }
        String certProvider = properties.getProperty(prefix + CRYPTO_CERT_PROVIDER);
        if (certProvider != null) {
            setCryptoProvider(certProvider);
        }
        String cpNameConstraintsProp =
                properties.getProperty(prefix + CRYPTO_CERT_PROVIDER_HANDLES_NAME_CONSTRAINTS);
        if (cpNameConstraintsProp != null) {
            certProviderHandlesNameConstraints = Boolean.parseBoolean(cpNameConstraintsProp);
        }

        //
        // Load the KeyStore
        //
        String alias = properties.getProperty(prefix + KEYSTORE_ALIAS);
        if (alias != null) {
            alias = alias.trim();
            setDefaultX509Identifier(alias);
        }
        String keyStoreLocation = properties.getProperty(prefix + KEYSTORE_FILE);
        if (keyStoreLocation == null) {
            keyStoreLocation = properties.getProperty(prefix + OLD_KEYSTORE_FILE);
        }
        if (keyStoreLocation != null) {
            keyStoreLocation = keyStoreLocation.trim();

            try (InputStream is = loadInputStream(loader, keyStoreLocation)) {
                String passwd = properties.getProperty(prefix + KEYSTORE_PASSWORD); // Liberty Change: Backport 4.x
                if (passwd != null) {
                    passwd = passwd.trim();
                    passwd = decryptPassword(passwd, passwordEncryptor);
                }
                String type = properties.getProperty(prefix + KEYSTORE_TYPE, KeyStore.getDefaultType());
                if (type != null) {
                    type = type.trim();
                }
                keystore = load(is, passwd, keystoreProvider, type);
                LOG.debug(
                    "The KeyStore {} of type {} has been loaded", keyStoreLocation, type
                );
                String privatePasswd = properties.getProperty(prefix + KEYSTORE_PRIVATE_PASSWORD);
                if (privatePasswd != null) {
                    privatePasswordSet = true;
                }
            }

            String privateKeyCachingProp = properties.getProperty(prefix + KEYSTORE_PRIVATE_KEY_CACHING);
            if (privateKeyCachingProp != null) {
                enablePrivateKeyCaching = Boolean.parseBoolean(privateKeyCachingProp);
            }
        } else {
            LOG.debug("The KeyStore is not loaded as KEYSTORE_FILE is null");
        }

        //
        // Load the TrustStore
        //
        String trustProvider = this.properties.getProperty(prefix + TRUSTSTORE_PROVIDER);
        if (trustProvider != null) {
            trustProvider = trustProvider.trim();

            if (trustProvider.length() == 0) {
                trustProvider = null;
            } else {
                setTrustProvider(trustProvider);
            }
        } else if (keystoreProvider != null) {
            // Fallback to keystore provider for compatibility reason
            trustProvider = keystoreProvider;
            setTrustProvider(trustProvider);
        }

        String trustStoreLocation = properties.getProperty(prefix + TRUSTSTORE_FILE);
        if (trustStoreLocation != null) {
            trustStoreLocation = trustStoreLocation.trim();

            try (InputStream is = loadInputStream(loader, trustStoreLocation)) {
                String passwd = properties.getProperty(prefix + TRUSTSTORE_PASSWORD); // Liberty Change: Backport 4.x
                if (passwd != null) {
                    passwd = passwd.trim();
                    passwd = decryptPassword(passwd, passwordEncryptor);
                }
                String type = properties.getProperty(prefix + TRUSTSTORE_TYPE, KeyStore.getDefaultType());
                if (type != null) {
                    type = type.trim();
                }
                truststore = load(is, passwd, trustProvider, type);
                LOG.debug(
                    "The TrustStore {} of type {} has been loaded", trustStoreLocation, type
                );
                loadCACerts = false;
            }
        } else {
            String loadCacerts = properties.getProperty(prefix + LOAD_CA_CERTS, "false");
            if (loadCacerts != null) {
                loadCacerts = loadCacerts.trim();
            }
            if (Boolean.valueOf(loadCacerts)) {
                String cacertsPath = (System.getProperty("java.home") + "/lib/security/cacerts").trim();
                try (InputStream is = Files.newInputStream(Paths.get(cacertsPath))) {
                    String cacertsPasswd = properties.getProperty(prefix + TRUSTSTORE_PASSWORD, "changeit");
                    if (cacertsPasswd != null) {
                        cacertsPasswd = cacertsPasswd.trim();
                        cacertsPasswd = decryptPassword(cacertsPasswd, passwordEncryptor);
                    }
                    truststore = load(is, cacertsPasswd, null, KeyStore.getDefaultType());
                    LOG.debug("CA certs have been loaded");
                    loadCACerts = true;
                }
            }
        }

        //
        // Load the CRL file(s)
        //
        String crlLocations = properties.getProperty(prefix + X509_CRL_FILE);
        if (crlLocations != null) {
            String[] splittedCrlsLocations = crlLocations.split(COMMA_SEPARATOR);
            List<X509CRL> crls = new ArrayList<>(splittedCrlsLocations.length);
            for (String crlLocation : splittedCrlsLocations) {
                try (InputStream is = loadInputStream(loader, crlLocation.trim())) {
                    CertificateFactory cf = getCertificateFactory();
                    X509CRL crl = (X509CRL)cf.generateCRL(is);
                    crls.add(crl);
                } catch (Exception e) {
                    LOG.debug(e.getMessage(), e);
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "ioError00");
                }
            }
            try {
                if (keystoreProvider == null || keystoreProvider.length() == 0) {
                    crlCertStore =
                            CertStore.getInstance(
                                    "Collection",
                                    new CollectionCertStoreParameters(crls)
                            );

                } else {
                    crlCertStore =
                            CertStore.getInstance(
                                    "Collection",
                                    new CollectionCertStoreParameters(crls),
                                    keystoreProvider
                            );
                }
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "ioError00");
            }
            LOG.debug("The CRL files {} have been loaded", crlLocations);
        }
    }

    /**
     * Load a KeyStore object as an InputStream, using the ClassLoader and location arguments
     */
    public static InputStream loadInputStream(ClassLoader loader, String location)
        throws WSSecurityException, IOException {
        return Loader.loadInputStream(loader, location);
    }


    /**
     * Loads the keystore from an <code>InputStream </code>.
     * <p/>
     *
     * @param input <code>InputStream</code> to read from
     * @throws WSSecurityException
     */
    protected KeyStore load(InputStream input, String storepass, String provider, String type)
        throws WSSecurityException {
        KeyStore ks = null;

        try {
            if (provider == null || provider.length() == 0) {
                ks = KeyStore.getInstance(type);
            } else {
                ks = KeyStore.getInstance(type, provider);
            }

            ks.load(input, storepass == null || storepass.length() == 0
                ? new char[0] : storepass.toCharArray());
        } catch (IOException | GeneralSecurityException e) {
            LOG.debug(e.getMessage(), e);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "failedCredentialLoad");
        }
        return ks;
    }

    //
    // Accessor methods
    //

    /**
     * Gets the Keystore that was loaded
     *
     * @return the Keystore
     */
    public KeyStore getKeyStore() {
        return keystore;
    }

    /**
     * Set the Keystore on this Crypto instance
     *
     * @param keyStore the Keystore to set
     */
    public void setKeyStore(KeyStore keyStore) {
        keystore = keyStore;
    }

    /**
     * Gets the trust store that was loaded by the underlying implementation
     *
     * @return the trust store
     */
    public KeyStore getTrustStore() {
        return truststore;
    }

    /**
     * Set the trust store on this Crypto instance
     *
     * @param trustStore the trust store to set
     */
    public void setTrustStore(KeyStore trustStore) {
        truststore = trustStore;
    }

    /**
     * Set the CertStore from which to obtain a list of CRLs for Certificate Revocation
     * checking.
     * @param crlCertStore the CertStore from which to obtain a list of CRLs for Certificate
     * Revocation checking.
     */
    public void setCRLCertStore(CertStore crlCertStore) {
        this.crlCertStore = crlCertStore;
    }

    /**
     * Get the CertStore from which to obtain a list of CRLs for Certificate Revocation
     * checking.
     * @return the CertStore from which to obtain a list of CRLs for Certificate
     * Revocation checking.
     */
    public CertStore getCRLCertStore() {
        return crlCertStore;
    }

    /**
     * Singleton certificate factory for this Crypto instance.
     * <p/>
     *
     * @return Returns a <code>CertificateFactory</code> to construct
     *         X509 certificates
     * @throws WSSecurityException
     */
    @Override
    public CertificateFactory getCertificateFactory() throws WSSecurityException {
        if (certificateFactory != null) {
            return certificateFactory;
        }

        String provider = getCryptoProvider();
        String keyStoreProvider = null;
        if (keystore != null) {
            keyStoreProvider = keystore.getProvider().getName();
        }

        try {
            if (provider == null || provider.length() == 0) {
                if (keyStoreProvider != null && keyStoreProvider.length() != 0) {
                    try {
                        certificateFactory =
                            CertificateFactory.getInstance(
                                "X.509", mapKeystoreProviderToCertProvider(keyStoreProvider)
                            );
                    } catch (Exception ex) {
                        LOG.debug("The keystore provider '" + keyStoreProvider + "' does not support X.509 because \""
                                + ex.getMessage() + "\". The JVM default provider will be tried out next", ex);
                        //Ignore, we'll just use the default since they didn't specify one.
                        //Hopefully that will work for them.
                    }
                }
                if (certificateFactory == null) {
                    certificateFactory = CertificateFactory.getInstance("X.509");
                }
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

    private String mapKeystoreProviderToCertProvider(String s) {
        if ("SunJSSE".equals(s)) {
            return "SUN";
        }
        return s;
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
    @Override
    public String getDefaultX509Identifier() throws WSSecurityException {
        if (super.getDefaultX509Identifier() != null) {
            return super.getDefaultX509Identifier();
        }

        if (keystore != null) {
            try {
                Enumeration<String> as = keystore.aliases();
                if (as.hasMoreElements()) {
                    String alias = as.nextElement();
                    if (!as.hasMoreElements()) {
                        setDefaultX509Identifier(alias);
                        return alias;
                    }
                }
            } catch (KeyStoreException ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, ex, "keystore"
                );
            }
        }
        return null;
    }

    //
    // Keystore-specific Crypto functionality methods
    //

    /**
     * Get an X509Certificate (chain) corresponding to the CryptoType argument. The supported
     * types are as follows:
     *
     * TYPE.ISSUER_SERIAL - A certificate (chain) is located by the issuer name and serial number
     * TYPE.THUMBPRINT_SHA1 - A certificate (chain) is located by the SHA1 of the (root) cert
     * TYPE.SKI_BYTES - A certificate (chain) is located by the SKI bytes of the (root) cert
     * TYPE.SUBJECT_DN - A certificate (chain) is located by the Subject DN of the (root) cert
     * TYPE.ALIAS - A certificate (chain) is located by an alias, which for this implementation
     * means an alias of the keystore or truststore.
     */
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (cryptoType == null) {
            return new X509Certificate[0]; // Liberty Change: Backport 4.x
        }
        CryptoType.TYPE type = cryptoType.getType();
        X509Certificate[] certs = new X509Certificate[0]; // Liberty Change: Backport 4.x
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
            certs = getX509Certificates(cryptoType.getAlias());
            break;
        case ENDPOINT:
            break;
        }
        return certs;
    }

    /**
     * Get the implementation-specific identifier corresponding to the cert parameter. In this
     * case, the identifier corresponds to a KeyStore alias.
     * @param cert The X509Certificate for which to search for an identifier
     * @return the identifier corresponding to the cert parameter
     * @throws WSSecurityException
     */
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        String identifier = null;

        if (keystore != null) {
            identifier = getIdentifier(cert, keystore);
        }

        if (identifier == null && truststore != null) {
            identifier = getIdentifier(cert, truststore);
        }

        return identifier;
    }

    /**
     * Gets the private key corresponding to the certificate.
     *
     * @param certificate The X509Certificate corresponding to the private key
     * @param callbackHandler The callbackHandler needed to get the password
     * @return The private key
     */
    public PrivateKey getPrivateKey(
        X509Certificate certificate,
        CallbackHandler callbackHandler
    ) throws WSSecurityException {
        if (keystore == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The keystore is null"});
        }
        if (callbackHandler == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The CallbackHandler is null"});
        }

        String identifier = getIdentifier(certificate, keystore);
        if (identifier == null) {
            try {
                String msg = "Cannot find key for certificate";
                String logMsg = createKeyStoreErrorMessage(keystore);
                LOG.error(msg + logMsg);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                              new Object[] {msg});
            } catch (KeyStoreException ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey",
                        new Object[] {ex.getMessage()}
                );
            }
        }
        String password = getPassword(identifier, callbackHandler);
        return getPrivateKey(identifier, password);
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
        if (keystore == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The keystore is null"});
        }
        if (callbackHandler == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The CallbackHandler is null"});
        }

        String identifier = getIdentifier(publicKey, keystore);
        if (identifier == null) {
            try {
                String msg = "Cannot find key for corresponding public key";
                String logMsg = createKeyStoreErrorMessage(keystore);
                LOG.error(msg + logMsg);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                              new Object[] {msg});
            } catch (KeyStoreException ex) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey",
                        new Object[] {ex.getMessage()}
                );
            }
        }
        String password = getPassword(identifier, callbackHandler);
        return getPrivateKey(identifier, password);
    }

    /**
     * Gets the private key corresponding to the identifier.
     *
     * @param identifier The implementation-specific identifier corresponding to the key
     * @param password The password needed to get the key
     * @return The private key
     */
    public PrivateKey getPrivateKey(
        String identifier,
        String password
    ) throws WSSecurityException {
        if (keystore == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The keystore is null"});
        }
        try {
            if (identifier == null || !keystore.isKeyEntry(identifier)) {
                String msg = "Cannot find key for alias: [" + identifier + "]";
                String logMsg = createKeyStoreErrorMessage(keystore);
                LOG.error(msg + logMsg);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                              new Object[] {msg});
            }
            String pwd = password;
            if (pwd == null && privatePasswordSet) {
                pwd = properties.getProperty(PREFIX + KEYSTORE_PRIVATE_PASSWORD);
                if (pwd == null) {
                    pwd = properties.getProperty(OLD_PREFIX + KEYSTORE_PRIVATE_PASSWORD);
                }
                if (pwd != null) {
                    pwd = pwd.trim();
                    pwd = decryptPassword(pwd, passwordEncryptor);
                }
            }
            if (enablePrivateKeyCaching) {
                Key privateKey = privateKeyCache.get(identifier);
                if (privateKey != null) {
                    return (PrivateKey) privateKey;
                }
            }

            Key keyTmp = keystore.getKey(identifier, pwd == null
                                         ? new char[]{} : pwd.toCharArray());
            if (!(keyTmp instanceof PrivateKey)) {
                String msg = "Key is not a private key, alias: [" + identifier + "]";
                String logMsg = createKeyStoreErrorMessage(keystore);
                LOG.error(msg + logMsg);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                              new Object[] {msg});
            }

            if (enablePrivateKeyCaching) {
                privateKeyCache.put(identifier, (PrivateKey) keyTmp);
            }
            return (PrivateKey) keyTmp;
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException ex) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey", new Object[] {ex.getMessage()}
            );
        }
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
            if (foundCerts != null && foundCerts.length > 0 
                && foundCerts[0] != null && foundCerts[0].equals(certs[0])) { // Liberty Change: Backport 4.x
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
        List<Certificate[]> foundIssuingCertChains = null;
        String issuerString = certs[0].getIssuerX500Principal().getName();
        if (certs.length == 1) {

            Object subject = convertSubjectToPrincipal(issuerString);

            if (keystore != null) {
                foundIssuingCertChains = getCertificates(subject, keystore, false);
            }

            //If we can't find the issuer in the keystore then look at the truststore
            if ((foundIssuingCertChains == null || foundIssuingCertChains.isEmpty()) && truststore != null) {
                foundIssuingCertChains = getCertificates(subject, truststore, true);
            }

            if (foundIssuingCertChains == null || foundIssuingCertChains.isEmpty()
                || foundIssuingCertChains.get(0).length < 1) {
                String subjectString = certs[0].getSubjectX500Principal().getName();
                LOG.debug(
                    "No certs found in keystore for issuer {} of certificate for {}",
                     issuerString, subjectString
                );
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "certpath", new Object[] {"No trusted certs found"}
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

            // Generate cert path
            if (foundIssuingCertChains != null && !foundIssuingCertChains.isEmpty()) {
                java.security.cert.CertPathValidatorException validatorException = null;
                // Try each potential issuing cert path for a match
                for (Certificate[] foundCertChain : foundIssuingCertChains) {
                    X509Certificate[] x509certs = new X509Certificate[foundCertChain.length + 1];
                    x509certs[0] = certs[0];
                    System.arraycopy(foundCertChain, 0, x509certs, 1, foundCertChain.length);

                    List<X509Certificate> certList = Arrays.asList(x509certs);
                    CertPath path = getCertificateFactory().generateCertPath(certList);

                    try {
                        validator.validate(path, param);
                        // We have a valid cert path at this point so break
                        validatorException = null;
                        break;
                    } catch (java.security.cert.CertPathValidatorException e) {
                        validatorException = e;
                    }
                }

                if (validatorException != null) {
                    throw validatorException;
                }
            } else {
                List<X509Certificate> certList = Arrays.asList(certs);
                CertPath path = getCertificateFactory().generateCertPath(certList);

                validator.validate(path, param);
            }
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

    @Override
    public void verifyTrust(X509Certificate[] certs, boolean enableRevocation,
                            Collection<Pattern> subjectCertConstraints,
                            Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        verifyTrust(certs, enableRevocation, subjectCertConstraints);
        if (!matchesIssuerDnPattern(certs[0], issuerCertConstraints)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    // Separated out to allow subclasses to override it
    protected PKIXParameters createPKIXParameters(
        Set<TrustAnchor> trustAnchors, boolean enableRevocation
    ) throws InvalidAlgorithmParameterException {
        PKIXParameters param = new PKIXParameters(trustAnchors);
        param.setRevocationEnabled(enableRevocation);
        if (enableRevocation && crlCertStore != null) {
            param.addCertStore(crlCertStore);
        }

        return param;
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
        // Search the keystore for the transmitted public key (direct trust). If not found
        // then search the truststore for the transmitted public key (direct trust)
        //
        if (!findPublicKeyInKeyStore(publicKey, keystore, false)
            && !findPublicKeyInKeyStore(publicKey, truststore, true)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    /**
     * Get an X509 Certificate (chain) according to a given serial number and issuer string.
     *
     * @param issuer The Issuer String
     * @param serialNumber The serial number of the certificate
     * @return an X509 Certificate (chain) corresponding to the found certificate(s)
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
        Certificate[] certs = null;
        if (keystore != null) {
            certs = getCertificates(issuerName, serialNumber, keystore, false);
        }

        //If we can't find the issuer in the keystore then look at the truststore
        if ((certs == null || certs.length == 0) && truststore != null) {
            certs = getCertificates(issuerName, serialNumber, truststore, true);
        }

        if (certs == null || certs.length == 0) {
            return new X509Certificate[0];
        }

        return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
    }

    /**
     * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
     * @param issuerRDN either an X500Principal or a BouncyCastle X509Name instance.
     * @param store The KeyStore
     * @return an X509 Certificate (chain)
     * @throws WSSecurityException
     */
    private Certificate[] getCertificates(
        Object issuerRDN,
        BigInteger serialNumber,
        KeyStore store,
        boolean truststore
    ) throws WSSecurityException {
        String keystore = "keystore";
        if (truststore) {
            keystore = "truststore";
        }
        LOG.debug("Searching {} for cert with issuer {} and serial {}", keystore, issuerRDN, serialNumber);
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
                    X509Certificate x509cert = (X509Certificate) certs[0];
                    LOG.debug("Keystore alias {} has issuer {} and serial {}", alias,
                              x509cert.getIssuerX500Principal().getName(), x509cert.getSerialNumber());
                    if (x509cert.getSerialNumber().compareTo(serialNumber) == 0) {
                        Object certName =
                            createBCX509Name(x509cert.getIssuerX500Principal().getName());
                        if (certName.equals(issuerRDN)) {
                            LOG.debug("Issuer Serial match found using keystore alias {}", alias);
                            return certs;
                        }
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }

        LOG.debug("No issuer serial match found in {}", keystore);
        return new Certificate[]{};
    }

    /**
     * Get an X509 Certificate (chain) according to a given Thumbprint.
     *
     * @param thumbprint The SHA1 thumbprint info bytes
     * @return the X509 Certificate (chain) that was found (can be null)
     * @throws WSSecurityException if problems during keystore handling or wrong certificate
     */
    private X509Certificate[] getX509Certificates(byte[] thumbprint) throws WSSecurityException {
        MessageDigest sha = null;

        try {
            // Liberty Change Start: FIPS default to SHA256
            sha = CryptoUtils.isFips140_3EnabledWithBetaGuard() 
                ? MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256) 
                : MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA1);
            // Liberty Change End
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "decoding.general"
            );
        }
        Certificate[] certs = null;
        if (keystore != null) {
            certs = getCertificates(thumbprint, keystore, sha, false);
        }

        //If we can't find the issuer in the keystore then look at the truststore
        if ((certs == null || certs.length == 0) && truststore != null) {
            certs = getCertificates(thumbprint, truststore, sha, true);
        }

        if (certs == null || certs.length == 0) {
            return new X509Certificate[0];
        }

        return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
    }

    /**
     * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
     * @param thumbprint
     * @param store The KeyStore
     * @return an X509 Certificate (chain)
     * @throws WSSecurityException
     */
    private Certificate[] getCertificates(
        byte[] thumbprint,
        KeyStore store,
        MessageDigest sha,
        boolean truststore
    ) throws WSSecurityException {
        String keystore = "keystore";
        if (truststore) {
            keystore = "truststore";
        }
        LOG.debug("Searching {} for cert using a SHA-1 thumbprint", keystore);
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
                    X509Certificate x509cert = (X509Certificate) certs[0];
                    try {
                        sha.update(x509cert.getEncoded());
                    } catch (CertificateEncodingException ex) {
                        throw new WSSecurityException(
                            WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, ex, "encodeError"
                        );
                    }
                    byte[] data = sha.digest();

                    if (Arrays.equals(data, thumbprint)) {
                        LOG.debug("Thumbprint match found using keystore alias {}", alias);
                        return certs;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }

        LOG.debug("No thumbprint match found in {}", keystore);
        return new Certificate[]{};
    }

    /**
     * Get an X509 Certificate (chain) according to a given SubjectKeyIdentifier.
     *
     * @param skiBytes The SKI bytes
     * @return the X509 certificate (chain) that was found (can be null)
     */
    private X509Certificate[] getX509CertificatesSKI(byte[] skiBytes) throws WSSecurityException {
        Certificate[] certs = null;
        if (keystore != null) {
            certs = getCertificates(skiBytes, keystore, false);
        }

        //If we can't find the issuer in the keystore then look at the truststore
        if ((certs == null || certs.length == 0) && truststore != null) {
            certs = getCertificates(skiBytes, truststore, true);
        }

        if (certs == null || certs.length == 0) {
            return new X509Certificate[0]; // Liberty Change: Backport 4.x
        }

        return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
    }

    /**
     * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
     * @param skiBytes
     * @param store The KeyStore
     * @return an X509 Certificate (chain)
     * @throws WSSecurityException
     */
    private Certificate[] getCertificates(
        byte[] skiBytes,
        KeyStore store,
        boolean truststore
    ) throws WSSecurityException {
        String keystore = "keystore";
        if (truststore) {
            keystore = "truststore";
        }
        LOG.debug("Searching {} for cert using Subject Key Identifier bytes", keystore);
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
                    X509Certificate x509cert = (X509Certificate) certs[0];
                    byte[] data = getSKIBytesFromCert(x509cert);
                    if (data.length == skiBytes.length && Arrays.equals(data, skiBytes)) {
                        LOG.debug("SKI match found using keystore alias {}", alias);
                        return certs;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }

        LOG.debug("No SKI match found in {}", keystore);
        return new Certificate[]{};
    }

    /**
     * Get an X509 Certificate (chain) according to a given DN of the subject of the certificate
     *
     * @param subjectDN The DN of subject to look for
     * @return An X509 Certificate (chain) with the same DN as given in the parameters
     * @throws WSSecurityException
     */
    private X509Certificate[] getX509CertificatesSubjectDN(String subjectDN) throws WSSecurityException {
        Object subject = convertSubjectToPrincipal(subjectDN);

        List<Certificate[]> certs = null;
        if (keystore != null) {
            certs = getCertificates(subject, keystore, false);
        }

        //If we can't find the issuer in the keystore then look at the truststore
        if ((certs == null || certs.isEmpty()) && truststore != null) {
            certs = getCertificates(subject, truststore, true);
        }

        if (certs == null || certs.isEmpty()) {
            return new X509Certificate[0]; // Liberty Change: Backport 4.x
        }

        // We just choose the first entry
        return Arrays.copyOf(certs.get(0), certs.get(0).length, X509Certificate[].class);
    }

    private Object convertSubjectToPrincipal(String subjectDN) {
        //
        // Convert the subject DN to a java X500Principal object first. This is to ensure
        // interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
        // Then convert it to a BouncyCastle X509Name, which will order the attributes of
        // the DN in a particular way (see WSS-168). If the conversion to an X500Principal
        // object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
        // back on a direct conversion to a BC X509Name
        //
        try {
            X500Principal subjectRDN = new X500Principal(subjectDN);
            return createBCX509Name(subjectRDN.getName());
        } catch (IllegalArgumentException ex) {
            return createBCX509Name(subjectDN);
        }
    }

    /**
     * Get an X509 Certificate (chain) that correspond to the identifier. For this implementation,
     * the identifier corresponds to the KeyStore alias.
     *
     * @param identifier The identifier that corresponds to the returned certs
     * @return an X509 Certificate (chain) that corresponds to the identifier
     */
    private X509Certificate[] getX509Certificates(String identifier) throws WSSecurityException {
        if (identifier == null) {
            return new X509Certificate[0]; // Liberty Change: Backport 4.x
        }
        Certificate[] certs = null;
        try {
            if (keystore != null) {
                // There's a chance that there can only be a set of trust stores
                certs = keystore.getCertificateChain(identifier);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate cert = keystore.getCertificate(identifier);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }
            }

            if (certs == null && truststore != null) {
                // Now look into the trust stores
                certs = truststore.getCertificateChain(identifier);
                if (certs == null) {
                    Certificate cert = truststore.getCertificate(identifier);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }
            }

            if (certs == null) {
                return new X509Certificate[0]; // Liberty Change: Backport 4.x
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
        }

        return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
    }

    /**
     * Find the Public Key in a keystore.
     */
    private boolean findPublicKeyInKeyStore(PublicKey publicKey, KeyStore keyStoreToSearch, boolean truststore) {
        if (keyStoreToSearch == null) {
            return false;
        }
        String keystore = "keystore";
        if (truststore) {
            keystore = "truststore";
        }
        LOG.debug("Searching {} for public key {}", keystore, publicKey);
        try {
            for (Enumeration<String> e = keyStoreToSearch.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                Certificate[] certs = keyStoreToSearch.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate cert = keyStoreToSearch.getCertificate(alias);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }

                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate
                    && publicKey.equals(((X509Certificate)certs[0]).getPublicKey())) {
                    LOG.debug("PublicKey match found using keystore alias {}", alias);
                    return true;
                }
            }
        } catch (KeyStoreException e) {
            return false;
        }

        LOG.debug("No PublicKey match found in {}", keystore);
        return false;
    }

    /**
     * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore. If multiple
     * certs match the Subject DN, then multiple cert chains are returned.
     * @param subjectRDN either an X500Principal or a BouncyCastle X509Name instance.
     * @param store The KeyStore
     * @return an X509 Certificate (chain)
     * @throws WSSecurityException
     */
    private List<Certificate[]> getCertificates(Object subjectRDN, KeyStore store, boolean truststore)
        throws WSSecurityException {
        String keystore = "keystore";
        if (truststore) {
            keystore = "truststore";
        }
        LOG.debug("Searching {} for cert with Subject {}", keystore, subjectRDN);
        List<Certificate[]> foundCerts = new ArrayList<>();
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
                    X500Principal foundRDN = ((X509Certificate)certs[0]).getSubjectX500Principal();
                    Object certName = createBCX509Name(foundRDN.getName());

                    if (subjectRDN.equals(certName)) {
                        LOG.debug("Subject certificate match found using keystore alias {}", alias);
                        foundCerts.add(certs);
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }

        if (foundCerts.isEmpty()) {
            LOG.debug("No Subject match found in {}", keystore);
        }
        return foundCerts;
    }

    private static String createKeyStoreErrorMessage(KeyStore keystore) throws KeyStoreException {
        Enumeration<String> aliases = keystore.aliases();
        StringBuilder sb = new StringBuilder(keystore.size() * 7);
        boolean firstAlias = true;
        while (aliases.hasMoreElements()) {
            if (!firstAlias) {
                sb.append(", ");
            }
            sb.append(aliases.nextElement());
            firstAlias = false;
        }
        String msg = " in keystore of type [" + keystore.getType()
            + "] from provider [" + keystore.getProvider()
            + "] with size [" + keystore.size() + "] and aliases: {"
            + sb.toString() + "}";
        return msg;
    }

    /**
     * Adds {@code TrustAnchor}s found in the provided key store to the set.
     * <p>
     * When the Trust Anchors are constructed, the value of the
     * {@link #CRYPTO_CERT_PROVIDER_HANDLES_NAME_CONSTRAINTS} property will be checked.
     * If it has been set to {@code true}, then {@code NameConstraint}s will be added
     * to their Trust Anchors; if unset or set to false, the Name Constraints
     * will be nulled out on their Trust Anchors.
     *
     * The default Sun PKIX Path Validator does not support Name Constraints on
     * Trust Anchors and will throw an InvalidAlgorithmParameterException if they
     * are provided. Other implementations may also be unsafe.
     *
     * @param set       the set to which to add the {@code TrustAnchor}s
     * @param keyStore  the store to search for {@code X509Certificate}s
     * @throws KeyStoreException if a problem occurs accessing the keyStore
     */
    protected void addTrustAnchors(Set<TrustAnchor> set, KeyStore keyStore)
            throws KeyStoreException, WSSecurityException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert =
                    (X509Certificate) keyStore.getCertificate(alias);
            if (cert != null) {
                if (certProviderHandlesNameConstraints) {
                    TrustAnchor anchor = new TrustAnchor(cert, getNameConstraints(cert));
                    set.add(anchor);
                } else {
                    TrustAnchor anchor = new TrustAnchor(cert, null);
                    set.add(anchor);
                }
            }
        }
    }

    /**
     * Get an implementation-specific identifier that corresponds to the X509Certificate. In
     * this case, the identifier is the KeyStore alias.
     * @param cert The X509Certificate corresponding to the returned identifier
     * @param store The KeyStore to search
     * @return An implementation-specific identifier that corresponds to the X509Certificate
     */
    private String getIdentifier(X509Certificate cert, KeyStore store)
        throws WSSecurityException {
        try {
            for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();

                Certificate[] certs = store.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate retrievedCert = store.getCertificate(alias);
                    if (retrievedCert != null) {
                        certs = new Certificate[]{retrievedCert};
                    }
                }

                if (certs != null && certs.length > 0 && certs[0].equals(cert)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
        }
        return null;
    }

    private String getIdentifier(PublicKey publicKey, KeyStore store)
        throws WSSecurityException {
        try {
            for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();

                Certificate[] certs = store.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate retrievedCert = store.getCertificate(alias);
                    if (retrievedCert != null) {
                        certs = new Certificate[]{retrievedCert};
                    }
                }

                if (certs != null && certs.length > 0 && certs[0].getPublicKey().equals(publicKey)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
        }
        return null;
    }

    /**
     * Get a password from the CallbackHandler
     * @param identifier The identifier to give to the Callback
     * @param cb The CallbackHandler
     * @return The password retrieved from the CallbackHandler
     * @throws WSSecurityException
     */
    private String getPassword(
        String identifier,
        CallbackHandler cb
    ) throws WSSecurityException {
        WSPasswordCallback pwCb =
            new WSPasswordCallback(identifier, WSPasswordCallback.DECRYPT);
        try {
            Callback[] callbacks = new Callback[]{pwCb};
            cb.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "noPassword", new Object[] {identifier}
            );
        }

        return pwCb.getPassword();
    }

    protected String decryptPassword(String password, PasswordEncryptor passwordEncryptor) {
        if (password.startsWith(ENCRYPTED_PASSWORD_PREFIX)
            && password.endsWith(ENCRYPTED_PASSWORD_SUFFIX)) {
            if (passwordEncryptor == null) {
                String error =
                    "The Crypto properties has an encrypted password, but no PasswordEncryptor is configured!";
                LOG.debug(error);
                return password;
            }
            String substring = password.substring(ENCRYPTED_PASSWORD_PREFIX.length(),
                                                  password.length() - 1);
            return passwordEncryptor.decrypt(substring);
        }

        return password;
    }

    public void setPasswordEncryptor(PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }

    public void clearCache() {
        if (enablePrivateKeyCaching) {
            privateKeyCache.clear();
        }
    }

    public boolean isEnablePrivateKeyCaching() {
        return enablePrivateKeyCaching;
    }

    public void setEnablePrivateKeyCaching(boolean enablePrivateKeyCaching) {
        this.enablePrivateKeyCaching = enablePrivateKeyCaching;
    }
}
