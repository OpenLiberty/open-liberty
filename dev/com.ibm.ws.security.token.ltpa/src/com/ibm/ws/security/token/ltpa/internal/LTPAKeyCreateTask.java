/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
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
        return PasswordUtil.passwordDecode(config.getKeyPassword()).getBytes();
    }

    private LTPAKeyInfoManager getPreparedLtpaKeyInfoManager() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(locService,
                                          config.getKeyFile(),
                                          getKeyPasswordBytes(),
                                          config.getRealm());
        return keyInfoManager;
    }

    @Sensitive
    private Map<String, Object> createTokenFactoryMap() {
        LTPAKeyInfoManager keyInfoManager = config.getLTPAKeyInfoManager();
        LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(keyInfoManager.getPrivateKey(config.getKeyFile()));
        LTPAPublicKey ltpaPublicKey = new LTPAPublicKey(keyInfoManager.getPublicKey(config.getKeyFile()));
        byte[] sharedKey = keyInfoManager.getSecretKey(config.getKeyFile());

        Map<String, Object> tokenFactoryMap = new HashMap<String, Object>();
        tokenFactoryMap.put(LTPAConstants.EXPIRATION, config.getTokenExpiration());
        tokenFactoryMap.put(LTPAConstants.SECRET_KEY, sharedKey);
        tokenFactoryMap.put(LTPAConstants.PUBLIC_KEY, ltpaPublicKey);
        tokenFactoryMap.put(LTPAConstants.PRIVATE_KEY, ltpaPrivateKey);
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

            Tr.info(tc, "LTPA_CONFIG_READY", TimestampUtils.getElapsedTimeNanos(start), config.getKeyFile());
            config.configReady();
        } catch (Exception e) {
            Tr.error(tc, "LTPA_CONFIG_ERROR", config.getKeyFile());
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the LTPA key.", t);
            }
            Tr.error(tc, "LTPA_KEY_CREATE_ERROR");
        }
    }
}
