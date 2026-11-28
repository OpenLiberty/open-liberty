/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.BadPaddingException;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.KeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.internal.LTPAKeyFileCreator;
import com.ibm.ws.security.token.ltpa.internal.LTPAKeyFileCreatorImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

/**
 * Load or create an LTPA keys file, something that looks like this:
 *
 * <pre>
 * #IBM WebSphere Application Server key file
 * #Tue Sep 11 16:15:40 EDT 2007
 * com.ibm.websphere.CreationDate=Tue Sep 11 16\:15\:40 EDT 2007
 * com.ibm.websphere.ltpa.version=1.0
 * com.ibm.websphere.CreationHost=localhost
 * com.ibm.websphere.ltpa.Realm=SecureRealm
 *
 *
 * com.ibm.websphere.ltpa.3DESKey=YJ8ARFn0k2S5S5LONNdZG/mLvfYxa4gH3/cGjIn+mR4\=
 * com.ibm.websphere.ltpa.PrivateKey=vzJcMLGvZqZqbrCGF7zTHAmXAhaZpuZ1XGT0iRq+9Y7VY29/UoIeJkunVyWUSmGrlfqD8kLc5jWUKBMynpg3tQqYKEls5iaO8DtI5FiGWE79gDUYzIkMjGei6S23KqE62Rq+
 * BcrjJv9XYcoGJhLvnE9wftBRrNeI6WPO44KywBSH0sgilyqOvxF87YumiCazbFsbCuBBlDh0daVvosM6zCfQGEsP
 * /e2AQRg4N6kkLbswaeE+i8AoNs2eIGpuicAx5avCgeBT8WwYUhkl3qDaYlR8/kHXOIOPt7/6oW//8yPpvWcHaxEdW4rZrdjH3TEh7CyVN6u6fS7CiOwgodJXrXPpLajqr6nFZxMSwMSyEcQ\=
 * com.ibm.websphere.ltpa.PublicKey=ANKHjHZGY0Ry2jG6kWAOOdGFr8IDhP3igXAAtKNRjhz1SuHcgLq0ZF+mA50pfRBFuFWGxa8WEPthfMyx/xEncHMcoakGXJH1woLL3Bp+LYd/
 * HlYYOHnLtmcWYQOPseqn638nkRWVpVsayIWx9jonjFJx+vbsi5ah3volxurVWZe/AQAB
 * <p>
 * com.ibm.websphere.ltpa.3DESKey_1=YJ8ARFn0k2S5S5LONNdZG/mLvfYxa4gH3/cGjIn+mR4\=
 * com.ibm.websphere.ltpa.PrivateKey_1=vzJcMLGvZqZqbrCGF7zTHAmXAhaZpuZ1XGT0iRq+9Y7VY29/UoIeJkunVyWUSmGrlfqD8kLc5jWUKBMynpg3tQqYKEls5iaO8DtI5FiGWE79gDUYzIkMjGei6S23KqE62Rq+
 * BcrjJv9XYcoGJhLvnE9wftBRrNeI6WPO44KywBSH0sgilyqOvxF87YumiCazbFsbCuBBlDh0daVvosM6zCfQGEsP
 * /e2AQRg4N6kkLbswaeE+i8AoNs2eIGpuicAx5avCgeBT8WwYUhkl3qDaYlR8/kHXOIOPt7/6oW//8yPpvWcHaxEdW4rZrdjH3TEh7CyVN6u6fS7CiOwgodJXrXPpLajqr6nFZxMSwMSyEcQ\=
 * com.ibm.websphere.ltpa.PublicKey_1=ANKHjHZGY0Ry2jG6kWAOOdGFr8IDhP3igXAAtKNRjhz1SuHcgLq0ZF+mA50pfRBFuFWGxa8WEPthfMyx/xEncHMcoakGXJH1woLL3Bp+LYd/
 * HlYYOHnLtmcWYQOPseqn638nkRWVpVsayIWx9jonjFJx+vbsi5ah3volxurVWZe/AQAB
 * </p>
 * </pre>
 *
 * Note:
 * key version start with 0 or no index. For example 3DESKey or 3DESKey_0
 * (key verion 0 is blank or 0)
 *
 * com.ibm.websphere.ltpa.version=1.0 - No FIPS
 * com.ibm.websphere.ltpa.version=2.0 - FIPS
 */
public class LTPAKeyInfoManager {

    private static final TraceComponent tc = Tr.register(LTPAKeyInfoManager.class);

    private static final String SECRETKEY = "secretkey";
    private static final String PRIVATEKEY = "privatekey";
    private static final String PUBLICKEY = "publickey";
    // PQC key cache identifiers (Issue #35556 - Task 2.3)
    private static final String MLDSA_PRIVATEKEY = "mldsaprivatekey";
    private static final String MLDSA_PUBLICKEY = "mldsapublickey";
    // ML-KEM key cache identifiers (Phase 4)
    private static final String MLKEM_PRIVATEKEY = "mlkemprivatekey";
    private static final String MLKEM_PUBLICKEY = "mlkempublickey";
    private static final String LTPA_KEYS_BACKUP_EXTENSION = ".defaultpassword.backup";

    private final List<String> importFileCache = new ArrayList<String>();
    private final Map<String, byte[]> keyCache = new Hashtable<String, byte[]>();
    private final Map<String, String> realmCache = new Hashtable<String, String>();
    private final String suffixNoFips = ".nofips";
    private final String suffixFips = ".fips";

    private static CopyOnWriteArrayList<LTPAValidationKeysInfo> ltpaValidationKeysInfos = new CopyOnWriteArrayList<LTPAValidationKeysInfo>();

    /**
     * Load the contents of the properties file.
     *
     * @param res The WsResource of the key import file.
     *
     * @return The properties
     * @throws TokenException
     */
    private final Properties loadPropertiesFile(WsResource res) throws IOException {

        Properties props = new Properties();
        InputStream is = res.get();
        try {
            props.load(is);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error loading properties; " + e);
            }
            throw e;
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return props;
    }

    /**
     * Loads the contents of the primary/validation LTPA key import file if necessary.
     *
     * @param primaryKeyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *                                 ${app.root}/config
     * @param primaryKeyPassword   The password of the LTPA keys
     * @param validationKeys       The validationKeys
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public synchronized final void prepareLTPAKeyInfo(WsLocationAdmin locService, String primaryKeyImportFile, @Sensitive byte[] primaryKeyPassword,
                                                      @Sensitive List<Properties> validationKeys, boolean tryToReEncryptLtpaKeys) throws Exception {
        if (!this.importFileCache.contains(primaryKeyImportFile)) {
            loadLtpaKeysFile(locService, primaryKeyImportFile, primaryKeyPassword, false, false, null, tryToReEncryptLtpaKeys);
        }
        if (validationKeys != null && !validationKeys.isEmpty()) {
            ltpaValidationKeysInfos.clear();
            //load validationKeys
            Iterator<Properties> validationKeysIterator = validationKeys.iterator();
            while (validationKeysIterator.hasNext()) {
                OffsetDateTime validUntilDateOdt = null;
                Properties vKeys = validationKeysIterator.next();
                String filename = (String) vKeys.get(LTPAConfiguration.CFG_KEY_VALIDATION_FILE_NAME);
                if (!this.importFileCache.contains(vKeys.get(LTPAConfiguration.CFG_KEY_VALIDATION_FILE_NAME))) {
                    String validUntilDate = ((String) vKeys.get(LTPAConfiguration.CFG_KEY_VALIDATION_VALID_UNTIL_DATE));
                    if (validUntilDate != null) {
                        try {
                            validUntilDateOdt = OffsetDateTime.parse(validUntilDate);
                            if (validUntilDateOdt != null && isValidUntilDateExpired(filename, validUntilDateOdt)) {
                                continue; //Skip this LTPA validationKeys
                            }
                        } catch (Exception e) {
                            Tr.error(tc, "LTPA_VALIDATION_KEYS_VALID_UNTIL_DATE_INVALID_FORMAT", validUntilDate, filename);
                            continue; //Skip this LTPA validationKeys
                        }
                    }

                    byte[] password = getKeyPasswordBytes(vKeys);
                    boolean isConfiguredValidationKey = Boolean.valueOf(vKeys.getProperty(LTPAConfiguration.INTERNAL_KEY_IS_CONFIGURED_VALIDATION_KEY));
                    loadLtpaKeysFile(locService, filename, password, true, isConfiguredValidationKey, validUntilDateOdt, tryToReEncryptLtpaKeys);
                }
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "importFileCache: " + importFileCache.toString());
            Tr.debug(this, tc, "keyCache: " + keyCache.toString());
            Tr.debug(this, tc, "realmCache: " + realmCache.toString());
            Tr.debug(this, tc, "number of ltpaValidationKeysInfos: " + ltpaValidationKeysInfos.size());
        }
    }

    /**
     * This function checks if the validUntilDate0dt has already passed the current time.
     * If so, then they key is expired, and will return true with a warning message.
     * Otherwise, the key is valid and will return false.
     * If the validUntilDateOdt is null, then the key is forever valid and will return false.
     *
     * @param filename
     * @param validUntilDateOdt
     *
     * @return
     */
    public boolean isValidUntilDateExpired(String filename, OffsetDateTime validUntilDateOdt) {
        OffsetDateTime currentTime = OffsetDateTime.now(validUntilDateOdt.getOffset());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current date: " + currentTime);
        }

        if (validUntilDateOdt.isBefore(currentTime)) {
            Tr.warning(tc, "LTPA_VALIDATION_KEYS_VALID_UNTIL_DATE_IS_IN_THE_PAST", validUntilDateOdt, filename);
            return true;
        } else {
            return false;
        }
    }

    @Sensitive
    byte[] getKeyPasswordBytes(@Sensitive Properties vKeys) {
        String password = (String) vKeys.get(LTPAConfiguration.CFG_KEY_VALIDATION_PASSWORD);
        return PasswordUtil.passwordDecode(password).getBytes();
    }

    /**
     * @param locService
     * @param keyImportFile
     * @param keyPassword
     * @param validationKey
     * @param validUntilDateOdt
     * @throws IOException
     * @throws Exception
     */
    private void loadLtpaKeysFile(WsLocationAdmin locService, String keyImportFile, @Sensitive byte[] keyPassword, boolean validationKey, boolean isConfiguredValidationKey,
                                  OffsetDateTime validUntilDateOdt, boolean tryToReEncryptLtpaKeys) throws IOException, Exception {
        // Need to load the key import file
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Loading LTPA " + (validationKey == true ? "validation" : "primary") + "Keys file: " + keyImportFile);
        }
        Properties props = null;
        //Check to see if the LTPA key import file exists, create the keys and file if not
        WsResource ltpaKeyFileResource = getLTPAKeyFileResource(locService, keyImportFile);

        if (ltpaKeyFileResource != null) {
            props = loadPropertiesFile(ltpaKeyFileResource);
            String version = props.getProperty(LTPAKeyFileUtility.LTPA_VERSION_PROPERTY);
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "LTPA key version: " + version);
            }
            if ((CryptoUtils.isFips140_3Enabled() && "1.0".equals(version)) ||
                (!CryptoUtils.isFips140_3Enabled() && "2.0".equals(version))) {
                if (validationKey) {
                    if ("1.0".equals(version))
                        Tr.warning(tc, "LTPA_VALIDATION_KEYS_NEED_TO_REGENERATE", keyImportFile);
                    else
                        Tr.warning(tc, "LTPA_FIPS_VALIDATION_KEYS_NEED_TO_REGENERATE", keyImportFile);
                    return;
                } else {
                    backupLtpaKeyFile(locService, keyImportFile, ltpaKeyFileResource, version);
                    if (restoreLtpaKeyFile(locService, keyImportFile, ltpaKeyFileResource)) {
                        props = loadPropertiesFile(ltpaKeyFileResource);
                    } else {
                        //regenerate the primary key
                        props = createPrimaryKeyFile(locService, keyImportFile, keyPassword);
                    }
                }
            }

        } else if (validationKey) { //validationKeys file does not exist so error
            Tr.error(tc, "LTPA_KEYS_FILE_DOES_NOT_EXIST", keyImportFile);
            return;
        } else { //Primary keys file does not exist so create the primary key
            props = createPrimaryKeyFile(locService, keyImportFile, keyPassword);
        }

        if (props == null || props.isEmpty()) {
            return;
        }
        String realm = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_REALM);
        String secretKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY);
        String privateKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY);
        String publicKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY);
        // PQC: Load ML-DSA keys if present (Issue #35556 - Task 2.3)
        // Try new property names first, fall back to legacy names for backward compatibility
        String mldsaPrivateKeyStr = props.getProperty("com.ibm.websphere.ltpa.pqc.PrivateKey");
        if (mldsaPrivateKeyStr == null) {
            mldsaPrivateKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLDSA_PRIVATEKEY);
        }
        
        String mldsaPublicKeyStr = props.getProperty("com.ibm.websphere.ltpa.pqc.PublicKey");
        if (mldsaPublicKeyStr == null) {
            mldsaPublicKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLDSA_PUBLICKEY);
        }
        
        String pqcAlgorithm = props.getProperty("com.ibm.websphere.ltpa.pqc.Algorithm");
        if (pqcAlgorithm == null) {
            pqcAlgorithm = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PQC_ALGORITHM);
        }
        
        String cryptoMode = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_CRYPTO_MODE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "PQC keys present: " + (mldsaPrivateKeyStr != null && mldsaPublicKeyStr != null));
                Tr.debug(tc, "PQC algorithm: " + pqcAlgorithm);
                Tr.debug(tc, "Crypto mode: " + cryptoMode);
        }

        byte[] secretKey, privateKey, publicKey;
        byte[][] keys;

        try {
            keys = decryptKeys(keyPassword, secretKeyStr, privateKeyStr, publicKeyStr);
        } catch (BadPaddingException e) {
            // only try to re-encrypt if it failed with keystore_password and it's not a configured validationKeys
            if (!tryToReEncryptLtpaKeys || (validationKey && isConfiguredValidationKey)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Error loading key; " + e);
                }
                throw e;
            }

            keys = reEncryptLtpaKey(locService, "WebAS".getBytes(), keyPassword,
                                    secretKeyStr, privateKeyStr, publicKeyStr,
                                    keyImportFile, ltpaKeyFileResource, e);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error loading key; " + e);
            }
            throw e;
        }

        secretKey = keys[0];
        if (secretKey != null) {
            this.keyCache.put(keyImportFile + SECRETKEY, secretKey);
        }
        privateKey = keys[1];
        if (privateKey != null) {
            this.keyCache.put(keyImportFile + PRIVATEKEY, privateKey);
        }
        publicKey = keys[2];
        if (publicKey != null) {
            this.keyCache.put(keyImportFile + PUBLICKEY, publicKey);
        }
        // PQC: Decrypt and cache ML-DSA keys if present (Issue #35556 - Task 2.3)
        if (mldsaPrivateKeyStr != null && !mldsaPrivateKeyStr.isEmpty()) {
                try {
                         KeyEncryptor encryptor = new KeyEncryptor(keyPassword);
                         byte[] encryptedMldsaPrivateKey = Base64Coder.base64DecodeString(mldsaPrivateKeyStr);
                         // Decrypt ML-DSA private key using the same password as RSA keys
                         byte[] mldsaPrivateKey = encryptor.decrypt(encryptedMldsaPrivateKey);
                         this.keyCache.put(keyImportFile + MLDSA_PRIVATEKEY, mldsaPrivateKey);

                         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                 Tr.debug(tc, "ML-DSA private key decrypted and cached, size: " + mldsaPrivateKey.length + " bytes");
                         }
                 } catch (Exception e) {
                         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                 Tr.debug(tc, "Error loading ML-DSA private key: " + e.getMessage());
                         }
                         throw e;
                 }
         }

         if (mldsaPublicKeyStr != null && !mldsaPublicKeyStr.isEmpty()) {
                 try {
                         byte[] mldsaPublicKey = Base64Coder.base64DecodeString(mldsaPublicKeyStr);
                         this.keyCache.put(keyImportFile + MLDSA_PUBLICKEY, mldsaPublicKey);

                         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                 Tr.debug(tc, "ML-DSA public key loaded and cached");
                         }
                 } catch (Exception e) {
                         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Error loading ML-DSA public key: " + e.getMessage());
                         }
                 }
          }
        
        // PQC: Load ML-KEM keys if present (Phase 4)
        String mlkemPrivateKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLKEM_PRIVATEKEY);
        String mlkemPublicKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLKEM_PUBLICKEY);
        String mlkemAlgorithm = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLKEM_ALGORITHM);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ML-KEM keys present: " + (mlkemPrivateKeyStr != null && mlkemPublicKeyStr != null));
            Tr.debug(tc, "ML-KEM algorithm: " + mlkemAlgorithm);
        }
        
        // Decrypt and cache ML-KEM private key if present
        if (mlkemPrivateKeyStr != null && !mlkemPrivateKeyStr.isEmpty()) {
            try {
                KeyEncryptor encryptor = new KeyEncryptor(keyPassword);
                byte[] encryptedMlkemPrivateKey = Base64Coder.base64DecodeString(mlkemPrivateKeyStr);
                // Decrypt ML-KEM private key using the same password as RSA keys
                byte[] mlkemPrivateKey = encryptor.decrypt(encryptedMlkemPrivateKey);
                this.keyCache.put(keyImportFile + MLKEM_PRIVATEKEY, mlkemPrivateKey);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ML-KEM private key decrypted and cached, size: " + mlkemPrivateKey.length + " bytes");
                    // Validate expected size for ML-KEM-768 (2400 bytes)
                    if (mlkemPrivateKey.length != 2400) {
                        Tr.debug(tc, "WARNING: ML-KEM private key size is " + mlkemPrivateKey.length + " bytes, expected 2400 bytes for ML-KEM-768");
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error loading ML-KEM private key: " + e.getMessage());
                }
                throw e;
            }
        }
        
        // Load and cache ML-KEM public key if present
        if (mlkemPublicKeyStr != null && !mlkemPublicKeyStr.isEmpty()) {
            try {
                byte[] mlkemPublicKey = Base64Coder.base64DecodeString(mlkemPublicKeyStr);
                this.keyCache.put(keyImportFile + MLKEM_PUBLICKEY, mlkemPublicKey);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ML-KEM public key loaded and cached, size: " + mlkemPublicKey.length + " bytes");
                    // Validate expected size for ML-KEM-768 (1184 bytes)
                    if (mlkemPublicKey.length != 1184) {
                        Tr.debug(tc, "WARNING: ML-KEM public key size is " + mlkemPublicKey.length + " bytes, expected 1184 bytes for ML-KEM-768");
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error loading ML-KEM public key: " + e.getMessage());
                }
            }
        }

        if (realm != null) {
            this.realmCache.put(keyImportFile, realm); //TODO: REALM? to support different realm name
        }

        this.importFileCache.add(keyImportFile);

        if (validationKey) {
            ltpaValidationKeysInfos.add(new LTPAValidationKeysInfo(keyImportFile, secretKey, privateKey, publicKey, validUntilDateOdt));
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "ValidationKeys: " + keyImportFile + " validUntilDate: " + validUntilDateOdt);
                Tr.debug(this, tc, "LTPAValidationKeysInfo size: " + ltpaValidationKeysInfos.size());
            }
        }
    }

    @Sensitive
    private byte[][] decryptKeys(@Sensitive byte[] keyPassword, @Sensitive String secretKeyStr, @Sensitive String privateKeyStr,
                                 @Sensitive String publicKeyStr) throws Exception {
        KeyEncryptor encryptor = new KeyEncryptor(keyPassword);
        byte[] secretKey, privateKey, publicKey;
        // Secret key
        if ((secretKeyStr == null) || (secretKeyStr.length() == 0)) {
            Tr.error(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_SECRETKEY);
            String formattedMessage = Tr.formatMessage(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_SECRETKEY);
            throw new IllegalArgumentException(formattedMessage);
        } else {
            byte[] keyEncoded = Base64Coder.base64DecodeString(secretKeyStr);
            secretKey = encryptor.decrypt(keyEncoded);
        }
        // Private key
        if ((privateKeyStr == null) || (privateKeyStr.length() == 0)) {
            Tr.error(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY);
            String formattedMessage = Tr.formatMessage(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY);
            throw new IllegalArgumentException(formattedMessage);
        } else {
            byte[] keyEncoded = Base64Coder.base64DecodeString(privateKeyStr);
            privateKey = encryptor.decrypt(keyEncoded);
        }
        // Public key
        if ((publicKeyStr == null) || (publicKeyStr.length() == 0)) {
            Tr.error(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY);
            String formattedMessage = Tr.formatMessage(tc, "LTPA_TOKEN_SERVICE_MISSING_KEY", LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY);
            throw new IllegalArgumentException(formattedMessage);
        } else {
            byte[] keyEncoded = Base64Coder.base64DecodeString(publicKeyStr);
            publicKey = keyEncoded;
        }
        return new byte[][] { secretKey, privateKey, publicKey };
    }

    @Sensitive
    private byte[][] reEncryptLtpaKey(WsLocationAdmin locService, @Sensitive byte[] keyPasswordToTry, @Sensitive byte[] keyPasswordToReEncryptWith,
                                      @Sensitive String secretKeyStr, @Sensitive String privateKeyStr, @Sensitive String publicKeyStr,
                                      String keyImportFile, WsResource ltpaKeyFileResource, Exception originalException) throws Exception {
        try {
            // failed with keystore_password... let's try again with the legacy default password
            byte[][] keys = decryptKeys(keyPasswordToTry, secretKeyStr, privateKeyStr, publicKeyStr);

            // successfully decrypted keys; backup and re-encrypt the keys using keystore_password
            Tr.info(tc, "LTPA_KEYS_REENCRYPT", keyImportFile);
            backupLtpaKeys(locService, ltpaKeyFileResource, keyImportFile);
            LTPAKeyFileCreator creator = new LTPAKeyFileCreatorImpl();
            creator.createLTPAKeysFile(locService, keyImportFile, keyPasswordToReEncryptWith, keys[0], keys[1], keys[2]);
            Tr.info(tc, "LTPA_KEYS_REENCRYPT_SUCCESS", keyImportFile);
            return keys;
        } catch (IOException e) {
            Tr.error(tc, "LTPA_KEYS_REENCRYPT_ERROR", keyImportFile, e.getMessage());
            String message = Tr.formatMessage(tc, "LTPA_KEYS_REENCRYPT_ERROR", keyImportFile, e.getMessage());
            throw new IOException(message);
        } catch (Exception e) {
            // didn't work with legacy default password; throw the original exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error loading key (retry): " + e + " Throwing original exception: " + originalException);
            }
            throw originalException;
        }
    }

    /**
     * Try to backup the ltpa.keys to ltpa.keys.defaultpassword.backup
     * If it's not available, then try ltpa.keys.defaultpassword.backup-1 til ltpa.keys.defaultpassword.backup-99
     * If none of those are available (extremely unlikely) then don't back up
     *
     * @param locService
     * @param ltpaKeyFileResource
     * @param keyImportFile
     * @throws IOException
     */
    @FFDCIgnore(Throwable.class)
    private void backupLtpaKeys(WsLocationAdmin locService, WsResource ltpaKeyFileResource, String keyImportFile) throws IOException {
        for (int i = 0; i < 100; i++) {
            String ltpaFileBackupLocation = keyImportFile + LTPA_KEYS_BACKUP_EXTENSION;
            if (i > 0) {
                ltpaFileBackupLocation += ("-" + i);
            }
            WsResource ltpaFileBackup = locService.resolveResource(ltpaFileBackupLocation);
            if (ltpaFileBackup.exists()) {
                continue;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Backup the LTPA key file: " + keyImportFile + " to: " + ltpaFileBackupLocation);
            }
            try (InputStream in = ltpaKeyFileResource.get()) {
                ltpaFileBackup.put(in);
            }
            return;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Could not find available location to backup the LTPA key file: " + keyImportFile);
        }
    }

    /**
     * @param locService
     * @param keyImportFile
     * @param ltpaKeyFileResource
     * @param version
     * @return
     * @throws IOException
     */
    private void backupLtpaKeyFile(WsLocationAdmin locService, String keyImportFile, WsResource ltpaKeyFileResource, String version) throws IOException {
        String suffix = suffixNoFips;
        if ("2.0".equals(version)) {
            suffix = suffixFips;
        }
        WsResource ltpaFileBackup = locService.resolveResource(keyImportFile + suffix);

        if (ltpaFileBackup.exists()) {
            ltpaFileBackup.delete();
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Backup the LTPA key file: " + keyImportFile + suffix);
        }
        ltpaKeyFileResource.moveTo(ltpaFileBackup);
    }

    /**
     * @param locService
     * @param keyImportFile
     * @WsResource ltpaKeyFileResource
     * @return
     * @throws IOException
     */
    private boolean restoreLtpaKeyFile(WsLocationAdmin locService, String keyImportFile, WsResource ltpaKeyFileResource) throws IOException {
        String suffix = suffixNoFips;
        if (CryptoUtils.isFips140_3Enabled()) {
            suffix = suffixFips;
        }

        WsResource ltpaFileBackup = locService.resolveResource(keyImportFile + suffix);
        if (ltpaFileBackup.exists()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Restoring the LTPA key file backup: " + keyImportFile + suffix);
            }
            ltpaFileBackup.moveTo(ltpaKeyFileResource);
            return true;
        }

        return false;
    }

    /**
     * @param locService
     * @param keyImportFile
     * @param keyPassword
     * @return
     * @throws Exception
     */
    private Properties createPrimaryKeyFile(WsLocationAdmin locService, String keyImportFile, @Sensitive byte[] keyPassword) throws Exception {
        long start = System.currentTimeMillis();
        Tr.info(tc, "LTPA_CREATE_KEYS_START");

        LTPAKeyFileCreator creator = new LTPAKeyFileCreatorImpl();
        Properties props = creator.createLTPAKeysFile(locService, keyImportFile, keyPassword);

        Tr.audit(tc, "LTPA_CREATE_KEYS_COMPLETE", TimestampUtils.getElapsedTime(start), keyImportFile);
        return props;
    }

    /**
     * Given the path to the LTPA key file return the WsResource for the file
     * if the file exists.
     *
     * @param ltpaKeyFile
     *
     * @return WsResource if the file exist, null if it does not.
     */
    final WsResource getLTPAKeyFileResource(WsLocationAdmin locService, String ltpaKeyFile) {
        WsResource ltpaFile = locService.resolveResource(ltpaKeyFile);
        if (ltpaFile != null && ltpaFile.exists()) {
            return ltpaFile;
        } else {
            return null;
        }
    }

    /**
     * Get the LTPA secret key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *                          ${App.root}/config
     * @return The LTPA secret key
     */
    @Sensitive
    public final byte[] getSecretKey(String keyImportFile) {
        return this.keyCache.get(keyImportFile + SECRETKEY);
    }

    /**
     * Get the LTPA private key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *                          ${App.root}/config
     * @return The LTPA private key
     */
    @Sensitive
    public final byte[] getPrivateKey(String keyImportFile) {
        return this.keyCache.get(keyImportFile + PRIVATEKEY);
    }

    /**
     * Get the LTPA public key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *                          ${App.root}/config
     * @return The LTPA public key
     */
    public final byte[] getPublicKey(String keyImportFile) {
        return this.keyCache.get(keyImportFile + PUBLICKEY);
    }

    /**
     * Get the LTPA realm.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *                          ${App.root}/config
     * @return The LTPA realm
     */
    final String getRealm(String keyImportFile) {
        return this.realmCache.get(keyImportFile);
    }

    public final List<LTPAValidationKeysInfo> getValidationLTPAKeys() {
        return ltpaValidationKeysInfos;
    }

    /**
         * Get ML-DSA private key from cache.
         *
         * @param keyImportFile The key file name
         * @return ML-DSA private key bytes or null if not present
         */
        public byte[] getMLDSAPrivateKey(String keyImportFile) {
                return this.keyCache.get(keyImportFile + MLDSA_PRIVATEKEY);
        }

        /**
         * Get ML-DSA public key from cache.
         *
         * @param keyImportFile The key file name
         * @return ML-DSA public key bytes or null if not present
         */
        public byte[] getMLDSAPublicKey(String keyImportFile) {
                return this.keyCache.get(keyImportFile + MLDSA_PUBLICKEY);
        }

        /**
         * Get ML-KEM private key from cache (Phase 4).
         *
         * @param keyImportFile The key file name
         * @return ML-KEM private key bytes or null if not present
         */
        public byte[] getMLKEMPrivateKey(String keyImportFile) {
                return this.keyCache.get(keyImportFile + MLKEM_PRIVATEKEY);
        }

        /**
         * Get ML-KEM public key from cache (Phase 4).
         *
         * @param keyImportFile The key file name
         * @return ML-KEM public key bytes or null if not present
         */
        public byte[] getMLKEMPublicKey(String keyImportFile) {
                return this.keyCache.get(keyImportFile + MLKEM_PUBLICKEY);
        }

}
