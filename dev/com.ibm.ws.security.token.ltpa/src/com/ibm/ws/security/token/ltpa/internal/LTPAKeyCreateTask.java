/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import java.security.Key;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.KeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;
import com.ibm.wsspi.security.crypto.SecretKeyResolver;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 * Asynchronous task to create LTPA keys.
 */
class LTPAKeyCreateTask implements Runnable {
    private static final TraceComponent tc = Tr.register(LTPAKeyCreateTask.class);
    private final WsLocationAdmin locService;
    private final LTPAConfigurationImpl config;
    private ServiceRegistration<LTPAConfiguration> reg = null;

    LTPAKeyCreateTask(WsLocationAdmin locService, LTPAConfigurationImpl config) {
        this.locService = locService;
        this.config = config;
    }

    @Sensitive
    byte[] getKeyPasswordBytes() {
        return PasswordUtil.passwordDecode(config.getPrimaryKeyPassword()).getBytes();
    }

    @Sensitive
    LTPAKeyEncryptor buildEncryptor() throws Exception {
        if (config.isUseEncryptionKey()) {
            // Determine which AES key version is active and build the appropriate encryptor.
            // Log an informational message so administrators can confirm which key source
            // is protecting the LTPA key material.
            if (AESKeyManager.isKeyConfigured(KeyVersion.AES_V2)) {
                SecretKeyResolver skr = AESKeyManager.getSecretKeyResolver();
                if (skr != null) {
                    // A hardware SecretKeyResolver is registered — this is the ICSF/CKDS path.
                    // getDescription() on ICSFSecretKeyResolver returns the ICSF key label.
                    Tr.info(tc, "LTPA_AES_ENCRYPTION_KEY_ICSF", skr.getDescription());
                } else {
                    // AES_V2 is configured via the wlp.aes.encryption.key system property.
                    Tr.info(tc, "LTPA_AES_ENCRYPTION_KEY_PROPERTY", AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY);
                }
                return new AesKeyEncryptor(AESKeyManager.getKeyViaResolver(KeyVersion.AES_V2));
            } else {
                // AES_V1 is configured via the wlp.password.encryption.key system property.
                Tr.info(tc, "LTPA_AES_ENCRYPTION_KEY_PROPERTY", AESKeyManager.NAME_WLP_PASSWORD_ENCRYPTION_KEY);
                return new AesKeyEncryptor(AESKeyManager.getKeyViaResolver(KeyVersion.AES_V1));
            }
        } else {
            return new KeyEncryptor(getKeyPasswordBytes());
        }
    }

    private LTPAKeyInfoManager getPreparedLtpaKeyInfoManager() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        if (config.isUseEncryptionKey()) {
            keyInfoManager.prepareLTPAKeyInfo(locService,
                                              config.getPrimaryKeyFile(),
                                              buildEncryptor(),
                                              config.getValidationKeys(),
                                              config.getTryToReEncryptLtpaKeys());
        } else {
            keyInfoManager.prepareLTPAKeyInfo(locService,
                                              config.getPrimaryKeyFile(),
                                              getKeyPasswordBytes(),
                                              config.getValidationKeys(),
                                              config.getTryToReEncryptLtpaKeys());
        }
        return keyInfoManager;
    }

    @Sensitive
    private Map<String, Object> createTokenFactoryMap() {
        LTPAKeyInfoManager keyInfoManager = config.getLTPAKeyInfoManager();
        LTPAPrivateKey primaryPrivateKey = new LTPAPrivateKey(keyInfoManager.getPrivateKey(config.getPrimaryKeyFile()));
        LTPAPublicKey primaryPublicKey = new LTPAPublicKey(keyInfoManager.getPublicKey(config.getPrimaryKeyFile()));
        byte[] primarySharedKey = keyInfoManager.getSecretKey(config.getPrimaryKeyFile());
        List<LTPAValidationKeysInfo> validationKeys = keyInfoManager.getValidationLTPAKeys();
        long expDiffAllowed = config.getExpirationDifferenceAllowed();

        Map<String, Object> tokenFactoryMap = new HashMap<String, Object>();
        tokenFactoryMap.put(LTPAConstants.EXPIRATION, config.getTokenExpiration());
        tokenFactoryMap.put(LTPAConstants.PRIMARY_SECRET_KEY, primarySharedKey);
        tokenFactoryMap.put(LTPAConstants.PRIMARY_PUBLIC_KEY, primaryPublicKey);
        tokenFactoryMap.put(LTPAConstants.PRIMARY_PRIVATE_KEY, primaryPrivateKey);
        tokenFactoryMap.put(LTPAConstants.VALIDATION_KEYS, validationKeys);
        tokenFactoryMap.put(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED, expDiffAllowed);
        return tokenFactoryMap;
    }

    private TokenFactory getTokenFactory() {
        Map<String, Object> tokenFactoryMap = createTokenFactoryMap();
        TokenFactory tokenFactory = new LTPAToken2Factory();
        tokenFactory.initialize(tokenFactoryMap);
        return tokenFactory;
    }

    /**
     * Create the required collaborators that the LTPAConfiguration will need.
     *
     * @throws Exception
     */
    void createRequiredCollaborators() throws Exception {
        config.setLTPAKeyInfoManager(getPreparedLtpaKeyInfoManager());
        config.setTokenFactory(getTokenFactory());
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void run() {
        try {
            long start = System.nanoTime();

            createRequiredCollaborators();
            if (reg == null) {
                BundleContext context = config.getBundleContext();
                if (context != null) {
                    reg = context.registerService(LTPAConfiguration.class,
                                                  config,
                                                  new Hashtable<String, String>());
                    config.setRegistration(reg);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The bundle context was null, we must have been deactivated while we were creating the keys");
                    }
                    return;
                }
            }

            Tr.info(tc, "LTPA_CONFIG_READY", TimestampUtils.getElapsedTimeNanos(start), config.getPrimaryKeyFile());
            config.configReady();
        } catch (Exception e) {
            Tr.error(tc, "LTPA_CONFIG_ERROR", config.getPrimaryKeyFile());
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the LTPA key.", t);
            }
            Tr.error(tc, "LTPA_KEY_CREATE_ERROR");
        }
    }
}
