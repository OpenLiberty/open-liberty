/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.audit.encryption;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditDecryptionException;
import com.ibm.wsspi.security.audit.AuditEncrypting;
import com.ibm.wsspi.security.audit.AuditEncryptionException;

public class AuditEncryptionImpl implements AuditEncrypting {

    private final static TraceComponent tc = Tr.register(com.ibm.ws.security.audit.encryption.AuditEncryptionImpl.class, null, "com.ibm.ejs.resources.security");
    private final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    static final String KEY_KEYSTORE_SERVICE_REF = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE_REF);

    String ciphers[] = null;
    AuditCrypto crypto = null;
    Key sharedkey = null;
    String serverName = null;
    int aliasIncrement = 1;
    private static AuditEncryptionImpl ae = null;
    private static String subjectDN = "CN=auditsigner, OU=SWG, O=IBM, C=US";
    private static String keyStoreName = "auditSignerKeyStore_";
    private static String certLabelPrefix = "auditcert";
    private static String CRYPTO_ALGORITHM = "SHA256withRSA";
    private final int signerKeyStoreIncrement = 1;
    private final ObjectName mgmScopeObjName = null;
    AuditKeyEncryptor encryptor = null;

    private String _name = null;
    private String _location = null;
    private String _type = null;
    private String _provider = null;
    private String _password = null;
    private String _alias = null;

    /**
     * <p>
     * The <code>getInstance</code> method returns initializes the AuditEncryption implementation
     * </p>
     *
     * @param String representing the non-fully qualified keystore name
     * @param String representing the path to the keystore
     * @param String representing the keystore type
     * @param String representing the keystore provider
     * @param String representing the password for the keystore
     * @param String representing the alias for the keystore entry
     * @return instance of the <code>AuditEncryption</code> object
     **/
    public static AuditEncryptionImpl getInstance(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                                                  String keyStorePassword, String keyAlias) throws AuditEncryptionException {
        try {
            if (ae == null)
                ae = new AuditEncryptionImpl(keyStoreName, keyStorePath, keyStoreType, keyStoreProvider, keyStorePassword, keyAlias);
            return ae;

        } catch (AuditEncryptionException e) {
            throw new AuditEncryptionException(e);
        }
    }

    public AuditEncryptionImpl(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                               String keyStorePassword, String keyAlias) throws AuditEncryptionException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "keyStoreName: " + keyStoreName + " keyStorePath: " + keyStorePath + " keyStoreType: " +
                         keyStoreType + " keyStoreProvider: " + keyStoreProvider + " keyStorePassword: " + keyStorePassword +
                         " keyAlias: " + keyAlias);
        }

        try {
            initialize(keyStoreName, keyStorePath, keyStoreType, keyStoreProvider, keyStorePassword, keyAlias);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception initializing AuditEncryptionImpl.", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditEncryptionImpl.constructor", "96", this);
        }
    }

    /**
     * <p>
     * The <code>initialize</code> method initializes the AuditEncryption implementation
     * </p>
     *
     * @param String representing the non-fully qualified keystore name
     * @param String representing the path to the keystore
     * @param String representing the keystore type
     * @param String representing the keystore provider
     * @param String representing the password for the keystore
     * @param String representing the alias for the keystore entry
     **/
    @Override
    public void initialize(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                           String keyStorePassword, String keyAlias) throws AuditEncryptionException {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        if (locationAdmin != null) {
            serverName = locationAdmin.getServerName();
        }

/*
 * boolean securityEnabled = CollaboratorHelperImpl.getCurrentSecurityEnabled();
 *
 * if (!securityEnabled) {
 * if (tc.isDebugEnabled())
 * Tr.debug(tc, "Security disabled, not initializing audit encryptor.");
 * return;
 * }
 */
        _name = keyStoreName;
        _location = keyStorePath;
        _type = keyStoreType;
        _provider = keyStoreProvider;
        _password = keyStorePassword;
        _alias = keyAlias;

        crypto = new AuditCrypto();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "_name: " + _name + " _location: " + _location + " _type: " +
                         _type + " _provider: " + _provider + " _keyStorePassword: " + _password +
                         " _alias: " + _alias);
        }

        //encryptor = new AuditKeyEncryptor(encodedPublicKey);

        long begin_time = 0;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing audit encryptor at " + new java.util.Date(System.currentTimeMillis()));
        }

    }

    /**
     * <p>
     * The <code>generateSharedKey</code> method generates and returns a symmetric key
     * </p>
     *
     * @return a Key object
     **/
    public Key generateSharedKey() throws KeyException {
        javax.crypto.spec.SecretKeySpec sharedKey = null;
        try {
            if (crypto != null) {
                try {
                    if (CryptoUtils.isFips140_3Enabled())
                        sharedKey = new javax.crypto.spec.SecretKeySpec(crypto.generateSharedKey(), 0, 32, CryptoUtils.CRYPTO_ALGORITHM_RSA);
                    else
                        sharedKey = new javax.crypto.spec.SecretKeySpec(crypto.generateSharedKey(), 0, 24, CryptoUtils.ENCRYPT_ALGORITHM_DESEDE);
                } catch (Exception me) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "me.getMessage: " + me.getMessage());
                }
            }

            if (sharedKey != null) {
                return sharedKey;
            } else {
                throw new com.ibm.websphere.crypto.KeyException("Key could not be generated.");
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Error generating key.", new Object[] { e });

            if (e instanceof KeyException)
                throw (KeyException) e;
            else
                throw new KeyException(e.getMessage(), e);
        }
    }

    /**
     * <p>
     * The <code>generateAliasForSharedKey</code> method creates an alias for a symmetric key, basing it off of
     * the cell, node and server names, post-pending the alias with an incremented id.
     * </p>
     *
     * @return a String representing the alias name
     **/
    public String generateAliasForSharedKey() {
        String alias = null;
        if (serverName != null) {
            alias = serverName + "Alias" + new Integer(aliasIncrement).toString();
        }
        aliasIncrement++;
        return alias;
    }

    /**
     * <p>
     * The <code>retrieveCertificate</code> method retrieves the Auditor's certificate used to encrypt
     * the audit records.
     * </p>
     *
     * @return an X509Certificate
     **/
    public X509Certificate retrieveCertificate() throws java.io.IOException {
        PublicKey publicKey = null;
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(_type, _provider);
            InputStream is = openKeyStore(_location);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "successfully opened the keystore at " + _location);
            ks.load(is, _password.toCharArray());
            if (tc.isDebugEnabled())
                Tr.debug(tc, "successfully loaded the keystore at " + _location);
            X509Certificate cert = (X509Certificate) ks.getCertificate(_alias);
            return cert;
        } catch (java.security.NoSuchProviderException ne) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: no such provider.", ne.getMessage());
            throw new IOException(ne.getMessage());
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: malformed URL", me.getMessage());
            throw new IOException(me.getMessage());
        } catch (java.security.KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ke.getMessage());
            throw new IOException(ke.getMessage());
        } catch (java.security.NoSuchAlgorithmException ae) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: no such algorithm", ae.getMessage());
            throw new IOException(ae.getMessage());
        } catch (java.security.cert.CertificateException ce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting certificate.", ce.getMessage());
            throw new IOException(ce.getMessage());
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            throw new IOException(ioe.getMessage());

        }
    }

    /**
     * <p>
     * The <code>retrievePublicKey</code> method retrieves the public key portion of the Auditor's certificate
     * used to encrypt the audit records.
     * </p>
     *
     * @return a Key object
     **/
    public Key retrievePublicKey() throws IOException {
        PublicKey publicKey = null;

        try {
            X509Certificate cert = retrieveCertificate();
            publicKey = cert.getPublicKey();
            return publicKey;
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", me.getMessage());
            throw new IOException(me.getMessage());
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            throw new IOException(ioe.getMessage());

        }
    }

    /**
     * <p>
     * The <code>encryptSharedKey</code> method encrypts a symmetric key with a public key.
     * </p>
     *
     * @return an encoded array of bytes representing the encrypted key
     **/
    public byte[] encryptSharedKey(Key sharedKey, Key pKey) throws java.io.IOException {
        byte[] encryptedSharedKey = null;
        if (sharedKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR!!! shared key is null!");
            }
            return null;
        }
        byte[] encodedPublicKey = pKey.getEncoded();
        byte[] encodedSharedKey = sharedKey.getEncoded();

        encryptor = new AuditKeyEncryptor(encodedPublicKey);

        encryptedSharedKey = encryptor.encrypt(encodedSharedKey);

        return encryptedSharedKey;
    }

    /**
     * <p>
     * The <code>decryptSharedKey</code> method decrypts the encrypted shared key.
     * </p>
     *
     * @return an encoded array of bytes representing the encrypted key
     **/
    public byte[] decryptSharedKey(byte[] sharedKey, Key pKey) throws java.io.IOException {
        byte[] decryptedSharedKey = null;
        if (sharedKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR!!! shared key is null!");
            }
            return null;
        }

        if (encryptor == null) {
            byte[] encodedPublicKey = pKey.getEncoded();
            encryptor = new AuditKeyEncryptor(encodedPublicKey);
        }

        byte[] encodedPublicKey = pKey.getEncoded();

        decryptedSharedKey = encryptor.decrypt(sharedKey);

        return decryptedSharedKey;
    }

    /**
     * <p>
     * The <code>encrypt</code> operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8").
     * An encrypted byte[] is returned.
     * </p>
     *
     * @param byte[] data to encrypt
     * @param java.security.Key shared key
     * @return byte[] of encrypted data
     * @throws com.ibm.wsspi.security.audit.AuditEncryptException
     **/
    @Override
    public byte[] encrypt(byte[] data, Key sharedKey) throws AuditEncryptionException {
        if (data == null) {
            Tr.error(tc, "security.audit.encryption.data.error");
            throw new AuditEncryptionException("Invalid data passed into the encryption algorithm.");
        }
        if (sharedKey == null) {
            Tr.error(tc, "security.audit.invalid.shared.key.error");
            throw new AuditEncryptionException("An invalid shared key was detected.");
        }

        AuditCrypto ac = new AuditCrypto();

        byte[] encryptedData = ac.encrypt(data, sharedKey.getEncoded());
        return encryptedData;
    }

    /**
     * <p>
     * The <code>decrypt</code> operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8").
     * A decrypted byte[] is returned.
     * </p>
     *
     * @param byte[] data to decrypt
     * @param java.security.Key shared key
     * @return byte[] of dcecrypted data
     * @throws com.ibm.wsspi.security.audit.AuditEncryptException
     **/
    @Override
    public byte[] decrypt(byte[] data, Key sharedKey) throws AuditDecryptionException {
        if (data == null) {
            Tr.error(tc, "security.audit.decryption.data.error");
            throw new AuditDecryptionException("Invalid data passed into the decryption algorithm.");
        }
        if (sharedKey == null) {
            Tr.error(tc, "security.audit.invalid.shared.key.error");
            throw new AuditDecryptionException("An invalid shared key was detected.");
        }
        AuditCrypto ac = new AuditCrypto();
        byte[] decryptedData = ac.decrypt(data, sharedKey.getEncoded());

        return decryptedData;

    }

    /*** openKeyStore method to open keystore in the form of a file or a url ***/
    protected static InputStream openKeyStore(String fileName) throws MalformedURLException, IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openKeyStore" + fileName);;
        try {
            OpenKeyStoreAction action = new OpenKeyStoreAction(fileName);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openKeyStore");
            return (InputStream) java.security.AccessController.doPrivileged(action);
        } catch (java.security.PrivilegedActionException e) {
            Exception ex = e.getException();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", new Object[] { ex });

            if (ex instanceof MalformedURLException)
                throw (MalformedURLException) ex;
            else if (ex instanceof IOException)
                throw (IOException) ex;

            throw new IOException(ex.getMessage());
        }
    }

    /**
     * This class is used to enable the code to read keystores.
     */
    static class OpenKeyStoreAction implements java.security.PrivilegedExceptionAction {
        private String file = null;

        public OpenKeyStoreAction(String fileName) {
            file = fileName;
        }

        @Override
        public Object run() throws MalformedURLException, IOException {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "OpenKeyStoreAction.run");

            InputStream fis = null;
            URL urlFile = null;

            // Check if the filename exists as a File.
            File kfile = new File(file);

            if (kfile.exists() && kfile.length() == 0) {
                throw new IOException("Keystore file exists, but is empty: " + file);
            } else if (!kfile.exists()) {
                // kfile does not exist as a File, treat as URL
                urlFile = new URL(file);
            } else {
                // kfile exists as a File
                urlFile = new URL("file:" + kfile.getCanonicalPath());
            }

            // Finally open the file.
            fis = urlFile.openStream();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "OpenKeyStoreAction.run");
            return fis;
        }
    }
}