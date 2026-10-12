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
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAHybridKeys;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;
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

    private LTPAKeyInfoManager getPreparedLtpaKeyInfoManager() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(locService,
                                          config.getPrimaryKeyFile(),
                                          getKeyPasswordBytes(),
                                          config.getValidationKeys(),
                                          config.getTryToReEncryptLtpaKeys());
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
        
        // Add hybrid PQC keys for Token Version 3
        String tokenVersion = config.getTokenVersion();
        if ("3".equals(tokenVersion)) {
            String primaryKeyFile = config.getPrimaryKeyFile();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to retrieve ML-DSA keys for: " + primaryKeyFile);
            }
            
            byte[] mldsaPrivateKey = keyInfoManager.getMLDSAPrivateKey(primaryKeyFile);
            byte[] mldsaPublicKey = keyInfoManager.getMLDSAPublicKey(primaryKeyFile);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ML-DSA private key retrieved: " + (mldsaPrivateKey != null ? mldsaPrivateKey.length + " bytes" : "null"));
                Tr.debug(tc, "ML-DSA public key retrieved: " + (mldsaPublicKey != null ? mldsaPublicKey.length + " bytes" : "null"));
            }
            
            if (mldsaPrivateKey != null && mldsaPublicKey != null) {
                // Retrieve ML-KEM keys (Phase 4)
                byte[] mlkemPrivateKey = keyInfoManager.getMLKEMPrivateKey(primaryKeyFile);
                byte[] mlkemPublicKey = keyInfoManager.getMLKEMPublicKey(primaryKeyFile);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ML-KEM private key retrieved: " + (mlkemPrivateKey != null ? mlkemPrivateKey.length + " bytes" : "null"));
                    Tr.debug(tc, "ML-KEM public key retrieved: " + (mlkemPublicKey != null ? mlkemPublicKey.length + " bytes" : "null"));
                }
                
                // Create hybrid keys object with RSA + ML-DSA + ML-KEM keys
                LTPAHybridKeys hybridKeys = new LTPAHybridKeys(
                    primaryPrivateKey.getEncoded(),  // RSA private key
                    primaryPublicKey.getEncoded(),   // RSA public key
                    mldsaPrivateKey,                 // ML-DSA private key
                    mldsaPublicKey,                  // ML-DSA public key
                    config.getMLDSAAlgorithm(),      // ML-DSA algorithm (e.g., "ML-DSA-65")
                    mlkemPrivateKey,                 // ML-KEM private key (Phase 4)
                    mlkemPublicKey,                  // ML-KEM public key (Phase 4)
                    config.getMLKEMAlgorithm()       // ML-KEM algorithm (e.g., "ML-KEM-768")
                );
                
                tokenFactoryMap.put(LTPAConstants.PRIMARY_HYBRID_KEYS, hybridKeys);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added hybrid PQC keys to token factory map");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ML-DSA keys not found, Token3Factory will not have hybrid keys");
                }
            }
        }
        
        return tokenFactoryMap;
    }

    private TokenFactory getTokenFactory() {
        Map<String, Object> tokenFactoryMap = createTokenFactoryMap();
        
        // Select token factory based on configured tokenVersion
        String tokenVersion = config.getTokenVersion();
        TokenFactory tokenFactory;
        
        if ("3".equals(tokenVersion)) {
            // LTPA Token Version 3 - Hybrid PQC (RSA + ML-DSA + ML-KEM)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating LTPAToken3Factory for hybrid PQC support");
            }
            tokenFactory = new LTPAToken3Factory();
        } else {
            // LTPA Token Version 2 - RSA only (default)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating LTPAToken2Factory for RSA-only support");
            }
            tokenFactory = new LTPAToken2Factory();
        }
        
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
