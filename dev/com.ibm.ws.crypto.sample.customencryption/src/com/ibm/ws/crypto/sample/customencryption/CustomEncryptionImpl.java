/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.crypto.sample.customencryption;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.wsspi.security.crypto.CustomPasswordEncryption;
import com.ibm.wsspi.security.crypto.EncryptedInfo;
import com.ibm.wsspi.security.crypto.PasswordDecryptException;
import com.ibm.wsspi.security.crypto.PasswordEncryptException;

/**
 */
@Component(service = CustomPasswordEncryption.class,
                immediate = true,
                name = "com.ibm.ws.crypto.sample.customencryption.CustomEncryptionImpl",
                configurationPolicy = ConfigurationPolicy.OPTIONAL,
                property = { "service.vendor=IBM" })
public class CustomEncryptionImpl implements CustomPasswordEncryption {
    private static final byte[] KEY = { 'T', 'h', 'i', 's', ' ', 'i', 's', ' ', 'a', ' ', 'k', 'e', 'y', '.', 'A', 'B' };
    private static final byte[] IV = { 'T', 'h', 'i', 's', ' ', 'i', 's', ' ', 'a', 'n', ' ', 'i', 'v', '.', 'a', 'b' };
    private static final Class<?> CLASS_NAME = CustomEncryptionImpl.class;
    private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName());

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("activate : cc :" + cc + " properties : " + props);
        }
    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("modify : properties : " + props);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("deactivate : cc :" + cc);
        }
    }

    /**
     * The encrypt operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8"). An encrypted byte[]
     * is returned from the implementation in the EncryptedInfo object.
     * Additionally, a logically key alias is returned in EncryptedInfo so which
     * is passed back into the decrypt method to determine which key was used to
     * encrypt this password. The WebSphere Application Server runtime has no
     * knowledge of the algorithm or key used to encrypt the data.
     * 
     * @param decrypted_bytes
     * @return com.ibm.wsspi.security.crypto.EncryptedInfo
     * @throws com.ibm.wsspi.security.crypto.PasswordEncryptException
     **/
    @Override
    public EncryptedInfo encrypt(byte[] input) throws PasswordEncryptException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("encrypt");
        }
        return new EncryptedInfo(encrypt(KEY, IV, input), null);
    }

    /**
     * The decrypt operation takes the EncryptedInfo object containing a byte[]
     * and the logical key alias and converts it to the decrypted byte[]. The
     * WebSphere Application Server runtime will convert the byte[] to a String
     * using new String (byte[], "UTF-8");
     * 
     * @param info
     * @return byte[]
     * @throws PasswordEncryptException
     * @throws com.ibm.wsspi.security.crypto.PasswordDecryptException
     **/
    @Override
    public byte[] decrypt(EncryptedInfo info) throws PasswordDecryptException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("decrypt : info : " + info);
        }
        byte[] input = null;
        if (info != null) {
            input = info.getEncryptedBytes();
        }
        return decrypt(KEY, IV, input);
    }

    /**
     * This is reserved for future use and is currently not called by the
     * WebSphere Application Server runtime.
     * 
     * @param initialization_data
     **/
    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(Map initialization_data) {}

    private static byte[] encrypt(byte[] key, byte[] iv, byte[] input) throws PasswordEncryptException {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new PasswordEncryptException("Exception is caught", e);
        }
    }

    private static byte[] decrypt(byte[] key, byte[] iv, byte[] input) throws PasswordDecryptException {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new PasswordDecryptException("Exception is caught", e);
        }
    }
}
