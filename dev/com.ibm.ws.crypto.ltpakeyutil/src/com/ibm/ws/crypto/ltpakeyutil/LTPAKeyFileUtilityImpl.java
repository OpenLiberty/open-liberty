/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * 
 */
public class LTPAKeyFileUtilityImpl implements LTPAKeyFileUtility {

    /** {@inheritDoc} */
    @Override
    public Properties createLTPAKeysFile(String keyFile, byte[] keyPasswordBytes) throws Exception {
        Properties ltpaProps = generateLTPAKeys(keyPasswordBytes, "defaultRealm");
        addLTPAKeysToFile(getOutputStream(keyFile), ltpaProps);
        return ltpaProps;
    }

    /**
     * Generates the LTPA keys and stores them into a Properties object.
     *
     * @param keyPasswordBytes
     * @param realm
     * @return
     * @throws Exception
     */
    protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, final String realm) throws Exception {
        Properties expProps = null;

        try {
            KeyEncryptor encryptor = new KeyEncryptor(keyPasswordBytes);
            LTPAKeyPair pair = LTPADigSignature.generateLTPAKeyPair();
            byte[] publicKey = pair.getPublic().getEncoded();
            byte[] privateKey = pair.getPrivate().getEncoded();
            byte[] encryptedPrivateKey = encryptor.encrypt(privateKey);
            byte[] sharedKey = LTPACrypto.generate3DESKey(); // key length is 24 for 3DES
            byte[] encryptedSharedKey = encryptor.encrypt(sharedKey);

            String tmpShared = Base64Coder.base64EncodeToString(encryptedSharedKey);
            String tmpPrivate = Base64Coder.base64EncodeToString(encryptedPrivateKey);
            String tmpPublic = Base64Coder.base64EncodeToString(publicKey);

            expProps = new Properties();

            expProps.put(KEYIMPORT_SECRETKEY, tmpShared);
            expProps.put(KEYIMPORT_PRIVATEKEY, tmpPrivate);
            expProps.put(KEYIMPORT_PUBLICKEY, tmpPublic);

            expProps.put(KEYIMPORT_REALM, realm);
            expProps.put(CREATION_HOST_PROPERTY, "localhost");
            expProps.put(LTPA_VERSION_PROPERTY, "1.0");
            expProps.put(CREATION_DATE_PROPERTY, (new java.util.Date()).toString());
        } catch (Exception e) {
            throw e;
        }

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
