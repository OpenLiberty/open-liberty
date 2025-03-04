/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditDecryptionException;
import com.ibm.wsspi.security.audit.AuditEncryptingException;
import com.ibm.wsspi.security.audit.AuditSigning;
import com.ibm.wsspi.security.audit.AuditSigningException;

public class AuditSigningImpl implements AuditSigning {

    private final static TraceComponent tc = Tr.register(com.ibm.ws.security.audit.encryption.AuditSigningImpl.class, null, "com.ibm.ejs.resources.security");

    private final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    AuditCrypto crypto = null;
    String serverName = null;
    int aliasIncrement = 1;
    private static AuditSigningImpl as = null;
    private static String subjectDN = "CN=auditsigner, OU=SWG, O=IBM, C=US";
    private static String keyStoreName = "auditSignerKeyStore_";
    private static String certLabelPrefix = "auditcert";

    private Signature signature = null;
    private final byte[] sigBytes = null;
    private final int signerKeyStoreIncrement = 1;
    private final ObjectName mgmScopeObjName = null;

    AuditKeyEncryptor encryptor = null;

    private String signerName = null;
    private final String signerType = null;
    private final String signerProvider = null;
    private String signerKeyFileLocation = null;
    private final String signerPassword = null;
    private String signerAlias = null;

    public AuditSigningImpl(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                            String keyStorePassword, String keyAlias) throws AuditSigningException {

        try {
            initialize(keyStoreName, keyStorePath, keyStoreType, keyStoreProvider, keyStorePassword, keyAlias);
        } catch (Exception e) {
            Tr.error(tc, "security.audit.signing.init.error", new Object[] { e });
        }
    }

    /**
     * <p>
     * The <code>initialize</code> method initializes the AuditSigning implementation
     * </p>
     **/
    @Override
    public void initialize(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                           String keyStorePassword, String keyAlias) throws AuditSigningException {

        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        if (locationAdmin != null) {
            serverName = locationAdmin.getServerName();
        }
        signerAlias = keyAlias;
        signerName = keyStoreName;
        signerKeyFileLocation = keyStorePath;

        crypto = new AuditCrypto();

        try {
            signature = Signature.getInstance(CryptoUtils.SIGNATURE_ALGORITHM_SHA512WITHRSA);
        } catch (Exception e) {
            Tr.error(tc, "security.audit.signing.init.error", new Object[] { e });
            throw new AuditSigningException(e.getMessage());
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing audit signer at " + new java.util.Date(System.currentTimeMillis()));
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
                sharedKey = new javax.crypto.spec.SecretKeySpec(crypto.generateSharedKey(), 0, CryptoUtils.AES_256_KEY_LENGTH_BYTES, CryptoUtils.ENCRYPT_ALGORITHM_AES);
            }

            if (sharedKey != null) {
                return sharedKey;
            } else {
                throw new com.ibm.websphere.crypto.KeyException("Key could not be generated.");
            }

        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditEncryptionImpl.generateKey", "98", this);
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
     * The <code>encryptSharedKey</code> method encrypts a symmetric key with a public key.
     * </p>
     *
     * @return an encoded array of bytes representing the encrypted key
     **/
    public byte[] encryptSharedKey(Key sharedKey, Key publicKey) throws java.io.IOException {
        byte[] encryptedSharedKey = null;
        if (sharedKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR!!! shared key is null!");
            }
            return null;
        }
        byte[] encodedPublicKey = publicKey.getEncoded();
        encryptor = new AuditKeyEncryptor(encodedPublicKey);
        byte[] encodedSharedKey = sharedKey.getEncoded();
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
     * The <code>retrieveSignerCertificate</code> method retrieves the Administrator's certificate used
     * to sign the audit records.
     * </p>
     *
     * @return an X509Certificate
     **/
    public X509Certificate retrieveSignerCertificate() throws Exception {

        // TO-DO: The real way is we get the keyStore value from
        // the audit Policy, get the audit truststore, retrieve
        // the public certificate and extract the public key from
        // the public certificate.   But since we don't have the WCCM
        // model change, we'll have to hardcode where the truststore is

        PublicKey publicKey = null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "signerAlias: " + signerAlias + " signerType: " + signerType + " signerProvider: " + signerProvider +
                         " signerKeyFileLocation: " + signerKeyFileLocation);
        }

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("JKS");
            InputStream is = openKeyStore(signerKeyFileLocation);
            X509Certificate cert = (X509Certificate) ks.getCertificate(signerAlias);

            return cert;
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: malformed URL", me.getMessage());
            throw new Exception(me.getMessage());
        } catch (java.security.KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ke.getMessage());
            throw new Exception(ke.getMessage());
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            throw new Exception(ioe.getMessage());

        }
    }

    /**
     * <p>
     * The <code>retrievePrivateSignerKey</code> method retrieves the private key associated with
     * the Adminstrator's certificate used to sign the audit records.
     * </p>
     *
     * @return a Key object
     **/
    public Key retrievePrivateSignerKey() throws Exception {
        KeyStore ks = null;
        Key k = null;
        try {
            ks = KeyStore.getInstance(signerType, signerProvider);
            InputStream is = openKeyStore(signerKeyFileLocation);
            ks.load(is, signerPassword.toCharArray());
            k = ks.getKey(signerAlias, signerPassword.toCharArray());
        } catch (java.security.NoSuchProviderException ne) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: no such provider.", ne.getMessage());
            throw new Exception(ne.getMessage());
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: malformed URL", me.getMessage());
            throw new Exception(me.getMessage());
        } catch (java.security.KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ke.getMessage());
            throw new Exception(ke.getMessage());
        } catch (java.security.UnrecoverableKeyException uke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", uke.getMessage());
            throw new Exception(uke.getMessage());
        } catch (java.security.NoSuchAlgorithmException ae) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: no such algorithm", ae.getMessage());
            throw new Exception(ae.getMessage());
        } catch (java.security.cert.CertificateException ce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting certificate.", ce.getMessage());
            throw new Exception(ce.getMessage());
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            throw new Exception(ioe.getMessage());

        }
        return k;
    }

    /**
     * <p>
     * The <code>retrievePublicSignerKey</code> method retrieves the public key associated with
     * the Adminstrator's certificate used to sign the audit records.
     * </p>
     *
     * @return a Key object
     **/
    public Key retrievePublicSignerKey() throws Exception {
        PublicKey publicKey = null;

        try {
            X509Certificate cert = retrieveSignerCertificate();
            publicKey = cert.getPublicKey();
            return publicKey;
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", me.getMessage());
            throw new Exception(me.getMessage());
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            throw new Exception(ioe.getMessage());
        } catch (java.lang.Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", e.getMessage());
            throw new Exception(e.getMessage());
        }
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
    public byte[] encrypt(byte[] data, Key sharedKey) throws AuditEncryptingException {
        if (data == null) {
            Tr.error(tc, "security.audit.encryption.data.error");
            throw new AuditEncryptingException("Invalid data passed into the encryption algorithm.");
        }
        if (sharedKey == null) {
            Tr.error(tc, "security.audit.invalid.shared.key.error");
            throw new AuditEncryptingException("Invalid shared key has been encountered.");
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

    /**
     * <p>
     * The <code>unsign</code> method unsigns the data with a key
     * </p>
     *
     * @param a byte array of data to be used to compute a message digest
     * @param the key used to unsign the signed data
     * @returns a byte array representing the signed message digest
     * @throws AuditSigningException
     **/
    public byte[] unsign(byte[] data, Key key) throws AuditSigningException {
        byte[] unsignedData = null;
        if (data != null) {
            try {
                unsignedData = decrypt(data, key);
            } catch (AuditDecryptionException ade) {
                throw new AuditSigningException(ade);
            }
        } else {
            Tr.error(tc, "security.audit.message.digest.error");
            throw new AuditSigningException("MessageDigest is invalid");
        }

        return (unsignedData);
    }

    /**
     * <p>
     * The <code>sign</code> method signs the data with a key
     * </p>
     *
     * @param a byte array of data to be used to compute a message digest
     * @param the key used to sign the message digest
     * @returns a byte array representing the signed message digest
     * @throws AuditSigningException
     **/
    @Override
    public byte[] sign(byte[] data, Key key) throws AuditSigningException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sign");

        byte[] messageDigest = null;
        byte[] signedData = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA512);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AuditSigningException(e);
        }
        if (data != null) {
            md.reset();
            md.update(data);
            messageDigest = md.digest();
        } else {
            Tr.error(tc, "security.audit.signing.data.error");
            throw new AuditSigningException("Invalid data passed into signing algorithm");
        }

        if (messageDigest != null) {
            try {
                signedData = encrypt(messageDigest, key);
            } catch (AuditEncryptingException aee) {
                throw new AuditSigningException(aee);
            }
        } else {
            Tr.error(tc, "security.audit.message.digest.error");
            throw new AuditSigningException("MessageDigest is invalid");
        }

        return (signedData);
    }

    /**
     * <p>
     * The <code>verify</code> method verifies the data is signed with a key
     * </p>
     *
     * @param a signed byte array of data
     * @param the key used to sign the byte array of data
     * @returns a boolean value based the successful verification of the data
     * @throws AuditSigningException
     **/
    @Override
    public boolean verify(byte[] data, Key key) throws AuditSigningException {
        if (signature != null) {
            try {
                signature.initVerify((PublicKey) key);
                signature.update(data);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "verify");
                return signature.verify(sigBytes);
            } catch (Exception ex) {
                throw new AuditSigningException(ex);
            }
        } else {
            String msg = "Signature is null.  Cannot verify data.";
            throw new AuditSigningException(msg);
        }
    }

    /**
     * <p>
     * The <code>verify</code> method verifies the data is signed with a key
     * </p>
     *
     * @param a signed byte array of data
     * @param the signature
     * @param the key used to sign the byte array of data
     * @returns a boolean value based the successful verification of the data
     * @throws AuditSigningException
     **/
    @Override
    public boolean verify(byte[] data, byte[] signature, Key key) throws AuditSigningException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "verify");

        byte[] messageDigest = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA512);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AuditSigningException(e);
        }
        if (data != null) {
            md.reset();
            md.update(data);
            messageDigest = md.digest();
        } else {
            throw new AuditSigningException("Invalid data passed into verifying algorithm");
        }

        if (messageDigest == null) {
            throw new AuditSigningException("MessageDigest is invalid");
        }

        byte[] unsignedData = unsign(signature, key);

        return Arrays.equals(messageDigest, unsignedData);
    }

    public String getSignerKeyFileLocation() {
        return signerKeyFileLocation;
    }

/*
 * public void initSignerKeyStore(String _signerKeyStoreName, String _signerKeyStoreScope, String _signerCertAlias) throws Exception {
 * String keyStoreScope = _signerKeyStoreScope;
 * String nodeName = null;
 * String certLabel = null;
 * KeyStoreInfo ksInfo = null;
 * AttributeList attrList = new AttributeList();
 *
 *
 * KeyStoreService service = null;
 * service = keyStoreServiceRef.getService();
 * try {
 * signerKeyFileLocation = service.getKeyStoreLocation(_signerKeyStoreName);
 * } catch (KeyStoreException e) {
 * throw new KeyStoreException(e);
 * }
 *
 *
 *
 * }
 */
/*
 * public boolean personalCertificateCreate(CertReqInfo ssCertInfo) throws Exception {
 * boolean certCreated = false;
 *
 * String alias = null;
 * String subjectDN = null;
 * int size = 0;
 * int validDays = 0;
 * KeyStoreInfo keyStoreInfo = null;
 * String keyStoreProvider = null;
 * String password = null;
 * String filePath = null;
 * String reqFilePath = null;
 *
 * //Get information needed to create self-signed Cert
 * subjectDN = ssCertInfo.getSubjectDN();
 * alias = ssCertInfo.getLabel();
 * size = ssCertInfo.getSize();
 * reqFilePath = ssCertInfo.getFilename();
 * filePath = "file://" + reqFilePath;
 *
 * //Get KeyStore information
 * keyStoreInfo = ssCertInfo.getKsInfo();
 * keyStoreProvider = keyStoreInfo.getProvider();
 * password = keyStoreInfo.getPassword();
 * WSKeyStoreRemotable wsksr = new WSKeyStoreRemotable(keyStoreInfo);
 *
 * PkSsCertificate ssCertificate = null;
 * X509Certificate certificate = null;
 * java.security.Key privateKey = null;
 *
 * String reqName = alias + "_certreq";
 *
 * KeyPair keyPair;
 * String locKeyType = "RSA";
 * String provider = "IBMJCE"; // Provider (IBM/SUN)
 * String randomNoGenerator = "IBMSecureRandom";
 *
 * List attrs = new ArrayList();
 * attrs.add("certreq@us.ibm.com");
 * attrs.add("CERTREQUEST");
 * attrs.add(filePath);
 *
 * String cn = subjectDN.substring(0, subjectDN.indexOf(","));
 * String dn = subjectDN.substring(subjectDN.indexOf(",") + 1);
 *
 * if (tc.isDebugEnabled()) {
 * Tr.debug(tc, "cn: " + cn + " dn: " + dn);
 * }
 *
 * try {
 *
 * //First create out keypair
 * KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(locKeyType, provider);
 * SecureRandom secureRandom = SecureRandom.getInstance(randomNoGenerator, provider);
 * keyPairGenerator.initialize(size, secureRandom);
 * keyPair = keyPairGenerator.generateKeyPair();
 *
 * //now create the self signed cert with a keyPair
 * Date deltaDate = new Date();
 * deltaDate.setTime(deltaDate.getTime() - (1 * 24 * 60 * 60 * 1000L));// minus one day <-- Cannot exceed "3 Days"
 * ssCertificate = PkSsCertFactory.newSsCert(size, subjectDN, 365, deltaDate, true, true, attrs,
 * null, null, "IBMJCE", keyPair);
 * if (ssCertificate != null) {
 * certificate = ssCertificate.getCertificate();
 * privateKey = ssCertificate.getKey();
 * String method = "setKeyEntryOverwrite";
 * Object[] parms = new Object[] { alias, privateKey, password.toCharArray(), new X509Certificate[] { certificate } };
 * wsksr.invokeKeyStoreCommand(method, parms, Boolean.TRUE);
 * certCreated = true;
 *
 * try {
 * Tr.audit(tc, "Self Signed Certificate: notBefore time: " + certificate.getNotBefore().toString() + " notAfter time: " + certificate.getNotAfter().toString());
 * } catch (Throwable t) {
 * // Do nothing. the point is not to fail just because we cannot generate an audit record
 * }
 *
 * } else {
 * String msg = "SelfSigned create failed.";
 * throw new Exception(msg);
 * }
 *
 * } catch (Throwable e) {
 * e.printStackTrace();
 * String msg = "Failed to create certificate";
 * throw new Exception(e.getMessage());
 * }
 * return certCreated;
 * }
 */
/*
 * public static AttributeList createKeyStoreAttrList(KeyStoreInfo ksInfo) {
 * AttributeList attrList = new AttributeList();
 * String name, location, type, password, provider, hostList, fileBased, readOnly, initializeAtStartup, scopeName;
 * Integer slotNumber = null;
 *
 * //Start with the reqired attributes
 * if ((name = ksInfo.getName()) != null)
 * attrList.add(new Attribute("name", name));
 * if ((location = ksInfo.getLocation()) != null)
 * attrList.add(new Attribute("location", location));
 * if ((type = ksInfo.getType()) != null)
 * attrList.add(new Attribute("type", type));
 * if ((password = ksInfo.getPassword()) != null)
 * attrList.add(new Attribute("password", password));
 * if ((provider = ksInfo.getProvider()) != null)
 * attrList.add(new Attribute("provider", provider));
 * if ((hostList = ksInfo.getHostList()) != null)
 * attrList.add(new Attribute("hostList", hostList));
 * if ((slotNumber = ksInfo.getSlot()) != null)
 * attrList.add(new Attribute("slot", slotNumber));
 *
 * attrList.add(new Attribute("managementScope", ksInfo.getScopeName()));
 * attrList.add(new Attribute("fileBased", ksInfo.getFileBased()));
 * attrList.add(new Attribute("readOnly", ksInfo.getReadOnly()));
 * attrList.add(new Attribute("initializeAtStartup", ksInfo.getInitializeAtStartup()));
 * attrList.add(new Attribute("createStashFileForCMS", ksInfo.getStashFile()));
 * attrList.add(new Attribute("useForAcceleration", ksInfo.getAccelerator()));
 * return (attrList);
 * }
 */

    /*** openKeyStore method to open keystore in the form of a file or a url ***/
    protected static InputStream openKeyStore(String fileName) throws MalformedURLException, IOException {
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
            return fis;
        }
    }

}