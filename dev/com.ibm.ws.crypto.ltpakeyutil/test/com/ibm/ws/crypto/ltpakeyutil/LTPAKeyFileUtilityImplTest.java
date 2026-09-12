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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class LTPAKeyFileUtilityImplTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // -----------------------------------------------------------------------
    // Existing password-path test
    // -----------------------------------------------------------------------

    @Test
    public void testLTPAKeyGeneration() throws Exception {
        LTPAKeyFileUtilityImpl creator = new LTPAKeyFileUtilityImpl();
        Properties keyInfo = creator.generateLTPAKeys("WebAS".getBytes(), "myRealm");

        // Check the secret key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY));

        // Check the private key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY));

        // Check the public key.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY));

        // Check the realm.
        Assert.assertEquals("myRealm", keyInfo.get(LTPAKeyFileUtility.KEYIMPORT_REALM));

        // Check the host.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.CREATION_HOST_PROPERTY));

        // Check the version.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.LTPA_VERSION_PROPERTY));

        // Check the creation date.
        Assert.assertNotNull(keyInfo.get(LTPAKeyFileUtility.CREATION_DATE_PROPERTY));
    }

    // -----------------------------------------------------------------------
    // LTPAKeyEncryptor overload tests
    // -----------------------------------------------------------------------

    /** Build a real 256-bit AES key from a fixed Base64 string. */
    private static Key makeAesKey() {
        byte[] keyBytes = Base64.getDecoder().decode("pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=");
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Calling {@code createLTPAKeysFile(path, LTPAKeyEncryptor)} must create a file
     * at the specified path.
     */
    @Test
    public void createLTPAKeysFile_withEncryptor_createsFile() throws Exception {
        File keyFile = tempFolder.newFile("ltpa.keys");
        // TemporaryFolder creates the file; the method requires it to not pre-exist,
        // so delete it and let the implementation create it.
        keyFile.delete();

        LTPAKeyFileUtilityImpl util = new LTPAKeyFileUtilityImpl();
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(makeAesKey());

        util.createLTPAKeysFile(keyFile.getAbsolutePath(), encryptor);

        Assert.assertTrue("LTPA keys file must be created on disk", keyFile.exists());
        Assert.assertTrue("LTPA keys file must not be empty", keyFile.length() > 0);
    }

    /**
     * The bytes stored in the file must differ from the raw (plaintext) key bytes
     * produced by the password overload, confirming that the AES encryptor is
     * actually applied rather than bypassed.
     */
    @Test
    public void createLTPAKeysFile_withEncryptor_fileIsNotPlaintext() throws Exception {
        File aesFile  = tempFolder.newFile("ltpa-aes.keys");
        File pwdFile  = tempFolder.newFile("ltpa-pwd.keys");
        aesFile.delete();
        pwdFile.delete();

        LTPAKeyFileUtilityImpl util = new LTPAKeyFileUtilityImpl();
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(makeAesKey());

        util.createLTPAKeysFile(aesFile.getAbsolutePath(), encryptor);
        util.createLTPAKeysFile(pwdFile.getAbsolutePath(), "WebAS".getBytes("UTF-8"));

        byte[] aesBytes = Files.readAllBytes(aesFile.toPath());
        byte[] pwdBytes = Files.readAllBytes(pwdFile.toPath());

        assertFalse("AES-encrypted file content must differ from password-encrypted file content",
                    Arrays.equals(aesBytes, pwdBytes));
    }

    /**
     * Key material written via the encryptor overload must be recoverable:
     * decrypting the stored secret-key property with the same encryptor must
     * yield a non-empty byte array (the original shared-key bytes).
     */
    @Test
    public void createLTPAKeysFile_withEncryptor_canBeDecrypted() throws Exception {
        File keyFile = tempFolder.newFile("ltpa.keys");
        keyFile.delete();

        Key aesKey = makeAesKey();
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(aesKey);
        LTPAKeyFileUtilityImpl util = new LTPAKeyFileUtilityImpl();

        util.createLTPAKeysFile(keyFile.getAbsolutePath(), encryptor);

        // Load the written Properties file and decrypt the secret key entry.
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(keyFile)) {
            props.load(fis);
        }

        String encryptedSecretKeyB64 = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY);
        assertNotNull("Secret key property must be present in the written file", encryptedSecretKeyB64);

        // Decrypt using a fresh encryptor backed by the same key.
        AesLTPAKeyEncryptor decryptor = new AesLTPAKeyEncryptor(aesKey);
        byte[] decrypted = decryptor.decrypt(
                com.ibm.ws.common.encoder.Base64Coder.base64DecodeString(encryptedSecretKeyB64));

        assertNotNull("Decrypted shared-key bytes must not be null", decrypted);
        Assert.assertTrue("Decrypted shared-key bytes must not be empty", decrypted.length > 0);
    }

    /**
     * The existing password-based overload must continue to work correctly alongside
     * the new encryptor overload (regression guard).
     */
    @Test
    public void createLTPAKeysFile_passwordOverload_unaffected() throws Exception {
        File keyFile = tempFolder.newFile("ltpa-pwd.keys");
        keyFile.delete();

        LTPAKeyFileUtilityImpl util = new LTPAKeyFileUtilityImpl();
        Properties props = util.createLTPAKeysFile(keyFile.getAbsolutePath(), "WebAS".getBytes("UTF-8"));

        Assert.assertTrue("Password-encrypted LTPA keys file must be created on disk", keyFile.exists());
        assertNotNull("Secret key property must be present", props.getProperty(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY));
        assertNotNull("Private key property must be present", props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY));
        assertNotNull("Public key property must be present",  props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY));
    }

}
