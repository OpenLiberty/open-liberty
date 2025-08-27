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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.util.FIPSUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;


/**
 * An implementation of PasswordEncryptor that relies on Jasypt's StandardPBEStringEncryptor to
 * encrypt and decrypt passwords. The default algorithm that is used is "PBEWithMD5AndTripleDES".
 */
public class JasyptPasswordEncryptor implements PasswordEncryptor {

    public static final String DEFAULT_ALGORITHM = 
        FIPSUtils.isFIPSEnabled() 
            ? "PBEWithHmacSHA512AndAES_256" : "PBEWithMD5AndTripleDES";

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(JasyptPasswordEncryptor.class);

    private final StandardPBEStringEncryptor passwordEncryptor;
    private CallbackHandler callbackHandler;

    public JasyptPasswordEncryptor(String password) {
        this(password, DEFAULT_ALGORITHM);
    }

    public JasyptPasswordEncryptor(String password, String algorithm) {
        passwordEncryptor = new StandardPBEStringEncryptor();
        passwordEncryptor.setPassword(password);
        passwordEncryptor.setAlgorithm(algorithm);
        if (FIPSUtils.isFIPSEnabled()) {
            passwordEncryptor.setSaltGenerator(new RandomSaltGenerator("PKCS11"));
            passwordEncryptor.setIvGenerator(new RandomIvGenerator("PKCS11"));
        }
    }

    public JasyptPasswordEncryptor(CallbackHandler callbackHandler) {
        this(callbackHandler, DEFAULT_ALGORITHM);
    }

    public JasyptPasswordEncryptor(CallbackHandler callbackHandler, String algorithm) {
        passwordEncryptor = new StandardPBEStringEncryptor();
        passwordEncryptor.setAlgorithm(algorithm);
        if (FIPSUtils.isFIPSEnabled()) {
            passwordEncryptor.setSaltGenerator(new RandomSaltGenerator("PKCS11"));
            passwordEncryptor.setIvGenerator(new RandomIvGenerator("PKCS11"));
        }
        this.callbackHandler = callbackHandler;
    }

    /**
     * Encrypt the given password
     * @param password the password to be encrypted
     * @return the encrypted password
     */
    public String encrypt(String password) {
        if (callbackHandler != null) {
            WSPasswordCallback pwCb =
                new WSPasswordCallback("", WSPasswordCallback.PASSWORD_ENCRYPTOR_PASSWORD);
            try {
                callbackHandler.handle(new Callback[]{pwCb});
            } catch (IOException | UnsupportedCallbackException e) {
                LOG.debug("Error in getting password: ", e);
            }
            if (pwCb.getPassword() != null) {
                passwordEncryptor.setPassword(pwCb.getPassword());
            }
        }
        return passwordEncryptor.encrypt(password);
    }

    /**
     * Decrypt the given encrypted password
     * @param encryptedPassword the encrypted password to decrypt
     * @return the decrypted password
     */
    public String decrypt(String encryptedPassword) {
        if (callbackHandler != null) {
            WSPasswordCallback pwCb =
                new WSPasswordCallback("", WSPasswordCallback.PASSWORD_ENCRYPTOR_PASSWORD);
            try {
                callbackHandler.handle(new Callback[]{pwCb});
            } catch (IOException | UnsupportedCallbackException e) {
                LOG.debug("Error in getting password: ", e);
            }
            if (pwCb.getPassword() != null) {
                passwordEncryptor.setPassword(pwCb.getPassword());
            }
        }
        return passwordEncryptor.decrypt(encryptedPassword);
    }

}
