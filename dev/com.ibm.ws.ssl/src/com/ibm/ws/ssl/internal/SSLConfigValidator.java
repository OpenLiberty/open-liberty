/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Validates the SSL configuration, using a fixed delay of 60 seconds
 * if a ScheduledExecutorService is provided during construction.
 */
@Component(service = SSLConfigValidator.class, property = "service.vendor=IBM")
public class SSLConfigValidator {
    private static final int VALIDATION_DELAY_IN_SECONDS = 60;
    private static final TraceComponent tc = Tr.register(SSLConfigValidator.class, "SSL", "com.ibm.ws.ssl.resources.ssl");
    private ConfigurationAdmin configAdmin;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduled;

    @Reference(service = ConfigurationAdmin.class)
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    @Reference(service = ScheduledExecutorService.class)
    protected void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    protected void unsetExecutorService(ScheduledExecutorService executorService) {
        this.executorService = null;
    }

    /**
     * Validate the SSL configuration.
     * </p>
     * Each request to validate configuration will, if possible, be delayed.
     * This delay is preferred as we need to wait for all of the configuration
     * is "available" before notifying the user, otherwise incorrect messages
     * may be logged.
     * </p>
     * When a delay is possible, multiple calls to validate will extend the
     * delay and replace the set of configuration information to be validated.
     */
    public void validate(Map<String, Object> map, Map<String, Map<String, Object>> repertoires, Map<String, WSKeyStore> keystores) {
        if (FrameworkState.isStopping()) {
            // Noop
        } else {
            Validator validator = new Validator(map, repertoires, keystores);
            if (executorService != null) {
                scheduleValidation(validator);
            } else {
                validator.run();
            }
        }
    }

    private void scheduleValidation(Validator validator) {
        if (scheduled != null) {
            scheduled.cancel(false);
        }
        this.scheduled = this.executorService.schedule(validator, VALIDATION_DELAY_IN_SECONDS, TimeUnit.SECONDS);
    }

    private class Validator implements Runnable {
        private final Map<String, Object> map;
        private final Map<String, Map<String, Object>> repertoires;
        private final Map<String, WSKeyStore> keystores;

        /**
         * @param map The global SSL configuration properties
         * @param repertoires
         * @param keystores
         */
        public Validator(Map<String, Object> map, Map<String, Map<String, Object>> repertoires, Map<String, WSKeyStore> keystores) {
            this.map = map;
            this.repertoires = repertoires;
            this.keystores = keystores;
        }

        @Override
        public void run() {
            validateNow();
        }

        private void validateNow() {
            String defaultSSLKeyStoreRef = getDefaultRepetorieKeyStore();
            if (defaultSSLKeyStoreRef != null) {
                if (LibertyConstants.DEFAULT_KEYSTORE_REF_ID.equals(defaultSSLKeyStoreRef)) {
                    // Handle the case where the defaultSSLConfig is pointing to defaultKeyStore
                    warnIfUsingUnresolvedDefaultConfiguration();
                } else {
                    // Handle the case where the defaultSSLConfig is pointing to something
                    // other than defaultKeyStore
                    reportErrorIfDefaultSSLConfigReferencesMissingKeystore(defaultSSLKeyStoreRef);
                }
            }
        }

        /**
         * Issue a warning if the defaultKeyStore does not exist and is expected to.
         * </p>
         * The defaultKeyStore won't be created unless a password is specified.
         * </p>
         * The default SSL repertoire will not be instantiated unless the referenced
         * keystore (defaultKeyStore) exists. So if we are expecting the default
         * repertoire but it doesn't exist, then we're missing defaultKeyStore configuration.
         */
        private void warnIfUsingUnresolvedDefaultConfiguration() {
            if (isUsingDefaultSSLRepertoire() &&
                defaultRepertoireIsNotAvailable() &&
                defaultKeyStoreIsNotAvailable()) {
                Tr.warning(tc, "ssl.defaultKeyStore.expected.CWPKI0817W", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
            }
        }

        /**
         * Determine if the global SSL properties are configured to use default SSL repertoire.
         *
         * @param map The global SSL configuration properties
         * @return {@code true} if the default SSL repertoire is in use, {@code false} otherwise
         */
        private boolean isUsingDefaultSSLRepertoire() {
            return map.get(Constants.SSLPROP_DEFAULT_ALIAS).equals(LibertyConstants.DEFAULT_SSL_CONFIG_ID);
        }

        private boolean defaultRepertoireIsNotAvailable() {
            return repertoires.get(LibertyConstants.DEFAULT_SSL_CONFIG_ID) == null;
        }

        private boolean defaultKeyStoreIsNotAvailable() {
            return keystores.get(LibertyConstants.DEFAULT_KEYSTORE_REF_ID) == null;
        }

        /**
         *
         */
        private void reportErrorIfDefaultSSLConfigReferencesMissingKeystore(String configuredKeyStore) {
            if (configuredKeyStore != null) {
                if (keystores.get(configuredKeyStore) == null) {
                    Tr.warning(tc, "ssl.defaultSSLConfig.noSuchKeyStore.CWPKI0818E", configuredKeyStore);
                }
            }
        }

        /**
         * Extract the configured (potentially unresovled) reference to the defaultSSLConfig's
         * keyStoreRef from ConfigAdmin.
         *
         * @return The configured keyStoreRef value for the defaultSSLConfig, or null if not available.
         */
        private String getDefaultRepetorieKeyStore() {
            if (configAdmin != null) {
                try {
                    Configuration[] configurations = configAdmin.listConfigurations("(" + org.osgi.framework.Constants.SERVICE_PID + "=com.ibm.ws.ssl.repertoire*)");
                    if (configurations != null) {
                        for (Configuration config : configurations) {
                            Dictionary<String, Object> properties = config.getProperties();
                            if (LibertyConstants.DEFAULT_SSL_CONFIG_ID.equals(properties.get("id"))) {
                                return (String) properties.get("keyStoreRef");
                            }
                        }
                    }
                } catch (IOException e) {
                    // Not expected! Let it FFDC
                } catch (InvalidSyntaxException e) {
                    // Not expected! Let it FFDC
                }
            }
            return null;
        }
    }

}
