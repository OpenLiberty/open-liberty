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
package org.apache.wss4j.stax.ext;

import java.lang.reflect.Constructor;
import java.security.KeyStore;
import java.security.cert.CertStore;
import java.util.Properties;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.util.Loader;
import org.apache.xml.security.stax.config.ConfigurationProperties;


/**
 */
class WSSCrypto {

    protected static final transient org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSCrypto.class);

    private Class<? extends Merlin> cryptoClass = Merlin.class;
    private Properties cryptoProperties;
    private Crypto cachedCrypto;
    private KeyStore cachedKeyStore;
    private KeyStore keyStore;
    private CertStore crlCertStore;
    private PasswordEncryptor passwordEncryptor;

    public Crypto getCrypto() throws WSSConfigurationException {

        if (keyStore == cachedKeyStore && cachedCrypto != null) {
            return cachedCrypto;
        }

        Merlin crypto = null;
        if (cryptoProperties != null) {
            try {
                Constructor<?> ctor =
                    cryptoClass.getConstructor(Properties.class, ClassLoader.class, PasswordEncryptor.class);
                crypto = (Merlin)ctor.newInstance(cryptoProperties,
                                                  Loader.getClassLoader(CryptoFactory.class),
                                                  passwordEncryptor);
                keyStore = crypto.getKeyStore();
            } catch (Exception e) {
                throw new WSSConfigurationException(WSSConfigurationException.ErrorCode.FAILURE, e, "signatureCryptoFailure");
            }
        } else {
            try {
                crypto = cryptoClass.newInstance();
                crypto.setDefaultX509Identifier(ConfigurationProperties.getProperty("DefaultX509Alias"));
                crypto.setCryptoProvider(ConfigurationProperties.getProperty("CertProvider"));
                crypto.setKeyStore(this.getKeyStore());
                crypto.setCRLCertStore(this.getCrlCertStore());
                crypto.setPasswordEncryptor(passwordEncryptor);
            } catch (Exception e) {
                throw new WSSConfigurationException(WSSConfigurationException.ErrorCode.FAILURE, e, "signatureCryptoFailure");
            }
        }

        cachedCrypto = crypto;
        cachedKeyStore = crypto.getKeyStore();
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        cachedCrypto = crypto;
        if (crypto instanceof Merlin) {
            keyStore = ((Merlin)crypto).getKeyStore();
            cachedKeyStore = keyStore;
        }
    }

    public Class<? extends Merlin> getCryptoClass() {
        return cryptoClass;
    }

    public void setCryptoClass(Class<? extends Merlin> cryptoClass) {
        this.cryptoClass = cryptoClass;
    }

    public Properties getCryptoProperties() {
        return cryptoProperties;
    }

    public void setCryptoProperties(Properties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public CertStore getCrlCertStore() {
        return crlCertStore;
    }

    public void setCrlCertStore(CertStore crlCertStore) {
        this.crlCertStore = crlCertStore;
    }

    public PasswordEncryptor getPasswordEncryptor() {
        return passwordEncryptor;
    }

    public void setPasswordEncryptor(PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }
}
