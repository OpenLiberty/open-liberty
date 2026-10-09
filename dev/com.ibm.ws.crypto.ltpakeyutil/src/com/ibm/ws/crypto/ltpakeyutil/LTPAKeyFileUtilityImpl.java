/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package com.ibm.ws.crypto.ltpakeyutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;

/**
 * 
 */
public class LTPAKeyFileUtilityImpl implements LTPAKeyFileUtility {

    /** {@inheritDoc} */
    @Override
    public Properties createLTPAKeysFile(String keyFile, byte[] keyPasswordBytes) throws Exception {
        return createLTPAKeysFile(keyFile, new PasswordLTPAKeyEncryptor(keyPasswordBytes));
    }

    /** {@inheritDoc} */
    @Override
    public Properties createLTPAKeysFile(String keyFile, LTPAKeyEncryptor encryptor) throws Exception {
        Properties ltpaProps = generateLTPAKeys(encryptor, "defaultRealm");
        addLTPAKeysToFile(getOutputStream(keyFile), ltpaProps);
        return ltpaProps;
    }

    /** {@inheritDoc} */
    @Override
    public Properties reEncryptLTPAKeysFile(String currentKeyFile, LTPAKeyEncryptor currentEncryptor,
                                            String newKeyFile, LTPAKeyEncryptor newEncryptor) throws Exception {
        // Load the existing key file
        Properties currentProps = new Properties();
        try (InputStream is = AccessController.doPrivileged(
                (PrivilegedExceptionAction<InputStream>) () -> new FileInputStream(new File(currentKeyFile)))) {
            currentProps.load(is);
        } catch (PrivilegedActionException e) {
            throw new IOException(e.getCause());
        }

        String secretKeyStr  = currentProps.getProperty(KEYIMPORT_SECRETKEY);
        String privateKeyStr = currentProps.getProperty(KEYIMPORT_PRIVATEKEY);
        String publicKeyStr  = currentProps.getProperty(KEYIMPORT_PUBLICKEY);
        String realm         = currentProps.getProperty(KEYIMPORT_REALM, "defaultRealm");

        if (secretKeyStr == null || secretKeyStr.isEmpty()) {
            throw new IllegalArgumentException("LTPA key file missing required property: " + KEYIMPORT_SECRETKEY);
        }
        if (privateKeyStr == null || privateKeyStr.isEmpty()) {
            throw new IllegalArgumentException("LTPA key file missing required property: " + KEYIMPORT_PRIVATEKEY);
        }
        if (publicKeyStr == null || publicKeyStr.isEmpty()) {
            throw new IllegalArgumentException("LTPA key file missing required property: " + KEYIMPORT_PUBLICKEY);
        }

        byte[] sharedKey  = currentEncryptor.decrypt(Base64Coder.base64DecodeString(secretKeyStr));
        byte[] privateKey = currentEncryptor.decrypt(Base64Coder.base64DecodeString(privateKeyStr));
        byte[] publicKey  = Base64Coder.base64DecodeString(publicKeyStr);

        // Re-encrypt with new encryptor and write to new file
        Properties newProps = generateLTPAKeys(newEncryptor, sharedKey, privateKey, publicKey, realm);
        addLTPAKeysToFile(getOutputStream(newKeyFile), newProps);
        return newProps;
    }

    /**
     * Generates LTPA keys using the supplied {@link LTPAKeyEncryptor}, generating
     * any key material that is passed as {@code null}.
     *
     * @param encryptor the encryptor used to protect the private and secret keys
     * @param realm     the realm name to embed in the key file
     * @return Properties containing the generated (encrypted) key material
     * @throws Exception
     */
    protected final Properties generateLTPAKeys(LTPAKeyEncryptor encryptor, final String realm) throws Exception {
        return generateLTPAKeys(encryptor, null, null, null, realm);
    }

    /**
     * Generates or re-encrypts LTPA key material using the supplied {@link LTPAKeyEncryptor}.
     * Pass {@code null} for {@code sharedKeyBytes}, {@code privateKeyBytes}, and
     * {@code publicKeyBytes} to generate fresh key material; pass existing plaintext
     * bytes to re-encrypt them.
     *
     * @param encryptor        the encryptor used to protect the private and secret keys
     * @param sharedKeyBytes   plaintext shared (3DES/AES) key bytes, or {@code null} to generate
     * @param privateKeyBytes  plaintext RSA private key bytes, or {@code null} to generate
     * @param publicKeyBytes   RSA public key bytes (stored as-is), or {@code null} to generate
     * @param realm            realm name to embed in the key file
     * @return Properties containing the (re-)encrypted key material
     * @throws Exception
     */
    protected final Properties generateLTPAKeys(LTPAKeyEncryptor encryptor,
                                                 byte[] sharedKeyBytes, byte[] privateKeyBytes,
                                                 byte[] publicKeyBytes, final String realm) throws Exception {
        if (publicKeyBytes == null && privateKeyBytes == null) {
            LTPAKeyPair pair = LTPADigSignature.generateLTPAKeyPair();
            publicKeyBytes = pair.getPublic().getEncoded();
            privateKeyBytes = pair.getPrivate().getEncoded();
        }
        if (sharedKeyBytes == null) {
            sharedKeyBytes = LTPACrypto.generateSharedKey();
        }

        Properties expProps = new Properties();
        expProps.put(KEYIMPORT_SECRETKEY,    Base64Coder.base64EncodeToString(encryptor.encrypt(sharedKeyBytes)));
        expProps.put(KEYIMPORT_PRIVATEKEY,   Base64Coder.base64EncodeToString(encryptor.encrypt(privateKeyBytes)));
        expProps.put(KEYIMPORT_PUBLICKEY,    Base64Coder.base64EncodeToString(publicKeyBytes));
        expProps.put(KEYIMPORT_REALM,        realm);
        expProps.put(CREATION_HOST_PROPERTY, "localhost");
        expProps.put(LTPA_VERSION_PROPERTY,  CryptoUtils.isFips140_3Enabled() ? "2.0" : "1.0");
        expProps.put(CREATION_DATE_PROPERTY, (new java.util.Date()).toString());
        return expProps;
    }

    /**
     * Obtain the OutputStream for the given file.
     * 
     * @param keyFile
     * @return
     * @throws IOException
     */
    private OutputStream getOutputStream(final String keyFile) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws IOException {
                    return new FileOutputStream(new File(keyFile));
                }
            });
        } catch (PrivilegedActionException e) {
            // Wrap the wrapped IOException from doPriv in an IOException and re-throw
            throw new IOException(e.getCause());
        }
    }

    /**
     * Write the LTPA key properties to the given OutputStream. This method
     * will close the OutputStream.
     *
     * @param keyImportFile The import file to be created
     * @param ltpaProps The properties containing the LTPA keys
     *
     * @throws TokenException
     * @throws IOException
     */
    protected void addLTPAKeysToFile(OutputStream os, Properties ltpaProps) throws Exception {
        try {
            // Write the ltpa key propeperties to
            ltpaProps.store(os, null);
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                }
        }

        return;
    }

}
