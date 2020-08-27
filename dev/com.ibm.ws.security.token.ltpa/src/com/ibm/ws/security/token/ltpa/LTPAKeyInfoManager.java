/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
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
 * com.ibm.websphere.ltpa.3DESKey=YJ8ARFn0k2S5S5LONNdZG/mLvfYxa4gH3/cGjIn+mR4\=
 * com.ibm.websphere.CreationHost=localhost
 * com.ibm.websphere.ltpa.PrivateKey=vzJcMLGvZqZqbrCGF7zTHAmXAhaZpuZ1XGT0iRq+9Y7VY29/UoIeJkunVyWUSmGrlfqD8kLc5jWUKBMynpg3tQqYKEls5iaO8DtI5FiGWE79gDUYzIkMjGei6S23KqE62Rq+
 * BcrjJv9XYcoGJhLvnE9wftBRrNeI6WPO44KywBSH0sgilyqOvxF87YumiCazbFsbCuBBlDh0daVvosM6zCfQGEsP
 * /e2AQRg4N6kkLbswaeE+i8AoNs2eIGpuicAx5avCgeBT8WwYUhkl3qDaYlR8/kHXOIOPt7/6oW//8yPpvWcHaxEdW4rZrdjH3TEh7CyVN6u6fS7CiOwgodJXrXPpLajqr6nFZxMSwMSyEcQ\=
 * com.ibm.websphere.ltpa.Realm=SecureRealm
 * com.ibm.websphere.ltpa.PublicKey=ANKHjHZGY0Ry2jG6kWAOOdGFr8IDhP3igXAAtKNRjhz1SuHcgLq0ZF+mA50pfRBFuFWGxa8WEPthfMyx/xEncHMcoakGXJH1woLL3Bp+LYd/
 * HlYYOHnLtmcWYQOPseqn638nkRWVpVsayIWx9jonjFJx+vbsi5ah3volxurVWZe/AQAB
 * </pre>
 */
public class LTPAKeyInfoManager {

    private static final TraceComponent tc = Tr.register(LTPAKeyInfoManager.class);

    private static final String SECRETKEY = "secretkey";
    private static final String PRIVATEKEY = "privatekey";
    private static final String PUBLICKEY = "publickey";

    private final List<String> importFileCache = new ArrayList<String>();
    private final Map<String, byte[]> keyCache = new Hashtable<String, byte[]>();
    private final Map<String, String> realmCache = new Hashtable<String, String>();

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
     * Loads the contents of the key import file if necessary.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
     * @param keyPassword The password of the LTPA keys
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public synchronized final void prepareLTPAKeyInfo(WsLocationAdmin locService, String keyImportFile, @Sensitive byte[] keyPassword) throws Exception {
        if (!this.importFileCache.contains(keyImportFile)) {
            // Need to load the key import file
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Loading keyfile [" + keyImportFile + "]");
            }
            Properties props = null;

            //Check to see if the LTPA key import file exists create the keys and file if not
            WsResource ltpaKeyFileResource = getLTPAKeyFileResource(locService, keyImportFile);
            if (ltpaKeyFileResource != null) {
                props = loadPropertiesFile(ltpaKeyFileResource);
            } else {
                long start = System.currentTimeMillis();
                Tr.info(tc, "LTPA_CREATE_KEYS_START");

                LTPAKeyFileCreator creator = new LTPAKeyFileCreatorImpl();
                props = creator.createLTPAKeysFile(locService, keyImportFile, keyPassword);

                Tr.audit(tc, "LTPA_CREATE_KEYS_COMPLETE", TimestampUtils.getElapsedTime(start), keyImportFile);
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
                this.realmCache.put(keyImportFile, realm);
            }
            this.importFileCache.add(keyImportFile);
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
            // The file does not exist so return null
            return null;
        }
    }

    /**
     * Get the LTPA secret key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
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
     *            ${App.root}/config
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
     *            ${App.root}/config
     * @return The LTPA public key
     */
    public final byte[] getPublicKey(String keyImportFile) {
        return this.keyCache.get(keyImportFile + PUBLICKEY);
    }

    /**
     * Get the LTPA realm.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
     * @return The LTPA realm
     */
    final String getRealm(String keyImportFile) {
        return this.realmCache.get(keyImportFile);
    }

}
