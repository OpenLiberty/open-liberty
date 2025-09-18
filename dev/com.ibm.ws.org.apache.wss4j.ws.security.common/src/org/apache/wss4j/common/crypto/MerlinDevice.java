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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRL;
import java.util.Collections;
import java.util.Properties;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * A Crypto implementation based on two Java KeyStore objects, one being the keystore, and one
 * being the truststore. This Crypto implementation extends the default Merlin implementation by
 * allowing loading of keystores using a null InputStream - for example on a smart-card device.
 */
public class MerlinDevice extends Merlin {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(MerlinDevice.class);

    public MerlinDevice() {
        super();
    }

    public MerlinDevice(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
        throws WSSecurityException, IOException {
        super(properties, loader, passwordEncryptor);
    }

    @Override
    public void loadProperties(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
        throws WSSecurityException, IOException {
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
        String provider = properties.getProperty(prefix + CRYPTO_KEYSTORE_PROVIDER);
        if (provider != null) {
            provider = provider.trim();
        }
        String certProvider = properties.getProperty(prefix + CRYPTO_CERT_PROVIDER);
        if (certProvider != null) {
            setCryptoProvider(certProvider);
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
        String keyStorePassword = properties.getProperty(prefix + KEYSTORE_PASSWORD, "security");
        if (keyStorePassword != null) {
            keyStorePassword = keyStorePassword.trim();
            keyStorePassword = decryptPassword(keyStorePassword, passwordEncryptor);
        }
        String keyStoreType = properties.getProperty(prefix + KEYSTORE_TYPE, KeyStore.getDefaultType());
        if (keyStoreType != null) {
            keyStoreType = keyStoreType.trim();
        }
        if (keyStoreLocation != null) {
            keyStoreLocation = keyStoreLocation.trim();

            try (InputStream is = loadInputStream(loader, keyStoreLocation)) {
                keystore = load(is, keyStorePassword, provider, keyStoreType);
                LOG.debug(
                    "The KeyStore {} of type {} has been loaded", keyStoreLocation, keyStoreType
                );
            }
        } else {
            keystore = load(null, keyStorePassword, provider, keyStoreType);
        }

        //
        // Load the TrustStore
        //
        String trustStorePassword = properties.getProperty(prefix + TRUSTSTORE_PASSWORD, "changeit");
        if (trustStorePassword != null) {
            trustStorePassword = trustStorePassword.trim();
            trustStorePassword = decryptPassword(trustStorePassword, passwordEncryptor);
        }
        String trustStoreType = properties.getProperty(prefix + TRUSTSTORE_TYPE, KeyStore.getDefaultType());
        if (trustStoreType != null) {
            trustStoreType = trustStoreType.trim();
        }
        String loadCacerts = properties.getProperty(prefix + LOAD_CA_CERTS, "false");
        if (loadCacerts != null) {
            loadCacerts = loadCacerts.trim();
        }
        String trustStoreLocation = properties.getProperty(prefix + TRUSTSTORE_FILE);
        if (trustStoreLocation != null) {
            trustStoreLocation = trustStoreLocation.trim();

            try (InputStream is = loadInputStream(loader, trustStoreLocation)) {
                truststore = load(is, trustStorePassword, provider, trustStoreType);
                LOG.debug(
                    "The TrustStore {} of type {} has been loaded", trustStoreLocation, trustStoreType
                );
                loadCACerts = false;
            }
        } else if (Boolean.valueOf(loadCacerts)) {
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
        } else {
            truststore = load(null, trustStorePassword, provider, trustStoreType);
        }
        //
        // Load the CRL file
        //
        String crlLocation = properties.getProperty(prefix + X509_CRL_FILE);
        if (crlLocation != null) {
            crlLocation = crlLocation.trim();

            try (InputStream is = loadInputStream(loader, crlLocation)) {
                CertificateFactory cf = getCertificateFactory();
                X509CRL crl = (X509CRL)cf.generateCRL(is);

                if (provider == null || provider.length() == 0) {
                    crlCertStore =
                        CertStore.getInstance(
                            "Collection",
                            new CollectionCertStoreParameters(Collections.singletonList(crl))
                        );
                } else {
                    crlCertStore =
                        CertStore.getInstance(
                            "Collection",
                            new CollectionCertStoreParameters(Collections.singletonList(crl)),
                            provider
                        );
                }
                LOG.debug("The CRL {} has been loaded", crlLocation);
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "failedCredentialLoad");
            }
        }
    }

}
