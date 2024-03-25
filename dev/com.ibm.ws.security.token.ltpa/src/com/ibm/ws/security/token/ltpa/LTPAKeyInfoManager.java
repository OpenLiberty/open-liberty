/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.KeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
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
 */
public class LTPAKeyInfoManager {

    private static final TraceComponent tc = Tr.register(LTPAKeyInfoManager.class);

    private static final String SECRETKEY = "secretkey";
    private static final String PRIVATEKEY = "privatekey";
    private static final String PUBLICKEY = "publickey";

    private final List<String> importFileCache = new ArrayList<String>();
    private final Map<String, byte[]> keyCache = new Hashtable<String, byte[]>();
    private final Map<String, String> realmCache = new Hashtable<String, String>();

    private static List<LTPAValidationKeysInfo> ltpaValidationKeysInfos = new ArrayList<LTPAValidationKeysInfo>();

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
                } catch (IOException e) {}
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
                                                      @Sensitive List<Properties> validationKeys) throws Exception {
        if (!this.importFileCache.contains(primaryKeyImportFile)) {
            loadLtpaKeysFile(locService, primaryKeyImportFile, primaryKeyPassword, false, null);
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
                    loadLtpaKeysFile(locService, filename, password, true, validUntilDateOdt);
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
    private void loadLtpaKeysFile(WsLocationAdmin locService, String keyImportFile, byte[] keyPassword, boolean validationKey,
                                  OffsetDateTime validUntilDateOdt) throws IOException, Exception {
        // Need to load the key import file
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Loading LTPA " + (validationKey == true ? "validation" : "primary") + "Keys file: " + keyImportFile);
        }
        Properties props = null;
        //Check to see if the LTPA key import file exists, create the keys and file if not
        WsResource ltpaKeyFileResource = getLTPAKeyFileResource(locService, keyImportFile);

        if (ltpaKeyFileResource != null) {
            props = loadPropertiesFile(ltpaKeyFileResource);
        } else if (validationKey) { //validationKeys file does not exist so error
            Tr.error(tc, "LTPA_KEYS_FILE_DOES_NOT_EXIST", keyImportFile);
            return;
        } else { //Primary keys file does not exist so create the primary key
            long start = System.currentTimeMillis();
            Tr.info(tc, "LTPA_CREATE_KEYS_START");

            LTPAKeyFileCreator creator = new LTPAKeyFileCreatorImpl();
            props = creator.createLTPAKeysFile(locService, keyImportFile, keyPassword);

            Tr.audit(tc, "LTPA_CREATE_KEYS_COMPLETE", TimestampUtils.getElapsedTime(start), keyImportFile);
        }

        if (props == null || props.isEmpty()) {
            return;
        }
        String realm = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_REALM);
        String secretKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_SECRETKEY);
        String privateKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY);
        String publicKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY);

        byte[] secretKey, privateKey, publicKey;
        try {
            KeyEncryptor encryptor = new KeyEncryptor(keyPassword);
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
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error loading key; " + e);
            }
            throw e;
        }

        if (secretKey != null) {
            this.keyCache.put(keyImportFile + SECRETKEY, secretKey);
        }
        if (privateKey != null) {
            this.keyCache.put(keyImportFile + PRIVATEKEY, privateKey);
        }
        if (publicKey != null) {
            this.keyCache.put(keyImportFile + PUBLICKEY, publicKey);
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

}
