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
package com.ibm.ws.security.token.ltpa;

import java.util.List;
import java.util.Properties;

import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 * Service class to indicate the LTPA configuration is available and
 * ready for use.
 */
public interface LTPAConfiguration {

    /**
     * The token keys file.
     */
    public static final String CFG_KEY_IMPORT_FILE = "keysFileName";

    /**
     * The token keys file password.
     */
    public static final String CFG_KEY_PASSWORD = "keysPassword"; // pragma: allowlist secret

    /**
     * The token expiration.
     */
    public static final String CFG_KEY_TOKEN_EXPIRATION = "expiration";

    public static final String CFG_KEY_REFRESH_THRESHOLD = "refreshThreshold";

    public static final String CFG_KEY_INACTIVITY_TIMEOUT = "inactivityTimeout";

    public static final String CFG_KEY_DYNAMIC_EXPIRATION_VALIDATION = "dynamicExpirationValidation";

    /**
     * The token keys file's monitor interval.
     */
    public static final String CFG_KEY_MONITOR_INTERVAL = "monitorInterval";

    /**
     * The Boolean to monitor the token keys file's directory.
     */
    public static final String CFG_KEY_MONITOR_VALIDATION_KEYS_DIR = "monitorValidationKeysDir";

    /**
     * The token keys file's update method or trigger.
     */
    public static final String CFG_KEY_UPDATE_TRIGGER = "updateTrigger";

    /**
     * The token validation keys.
     */
    static final String CFG_KEY_VALIDATION_KEYS = "validationKeys";

    /**
     * The token validation keys file(s).
     */
    static final String CFG_KEY_VALIDATION_FILE_NAME = "fileName";

    /**
     * The token validation keys file password.
     */
    static final String CFG_KEY_VALIDATION_PASSWORD = "password";

    /**
     * The the date-time to stop using the token validation keys.
     */
    static final String CFG_KEY_VALIDATION_VALID_UNTIL_DATE = "validUntilDate";

    /**
     * Internal property used to distinguish configured validation keys from non-configured validation keys.
     * Configured validation keys are explicitly defined in the server.xml using <validationKeys /> and require a password.
     * Non-configured validation keys are picked up when <ltpa monitorValidationKeysDir="true" /> is set and uses the same password as the primary ltpa key.
     *
     * Currently only used to determine if we should re-encrypt the validation key when the primary ltpa key is re-encrypted and it is a non-configured validation key.
     */
    static final String INTERNAL_KEY_IS_CONFIGURED_VALIDATION_KEY = "isConfiguredValidationKey";

    /**
     * @return TokenFactory instance corresponding to this LTPA configuration
     */
    TokenFactory getTokenFactory();

    /**
     * @return LTPAKeyInfoManager instance corresponding to this LTPA configuration
     */
    LTPAKeyInfoManager getLTPAKeyInfoManager();

    /**
     * @return LTPA key file
     */
    String getPrimaryKeyFile();

    /**
     * @return LTPA key password
     */
    String getPrimaryKeyPassword();

    /**
     * @return boolean for try to re-encrypt ltpa keys
     */
    boolean getTryToReEncryptLtpaKeys();

    /**
     * @return LTPA expiration
     */
    long getTokenExpiration();

    /**
     * Returns the refresh threshold for LTPA tokens in minutes.
     * When the time remaining until the token's <em>inactivity timeout</em> falls at or
     * below this threshold, the token is proactively refreshed: a new token is issued
     * with the creation time reset to now (restarting the inactivity window) while the
     * absolute expiration from the original token is preserved.
     * <p>
     * Note: this threshold is measured against the <strong>inactivity window</strong>,
     * not against the total token lifetime ({@code expiration}). It must be configured
     * to a value less than {@code inactivityTimeout}.
     * <p>
     * This is a beta feature and is only available when running in beta mode.
     *
     * @return refresh threshold in minutes
     * @ibm-api
     */
    long getRefreshThreshold();

    /**
     * Returns the inactivity timeout for LTPA tokens in minutes.
     * The token expires after this period of inactivity (measured from the token's
     * creation time, which is reset on each refresh). The inactivity timeout is always
     * capped at the absolute expiration so it can never extend beyond the token's hard
     * deadline. When the remaining time until inactivity timeout falls at or below
     * {@code refreshThreshold}, a new token is issued with a fresh inactivity window.
     * <p>
     * This is a beta feature and is only available when running in beta mode.
     *
     * @return inactivity timeout in minutes, or 0 if disabled
     * @ibm-api
     */
    long getInactivityTimeout();

    /**
     * Returns whether dynamic expiration validation is enabled.
     * <p>
     * When {@code true}:
     * <ul>
     *   <li>On token creation: the expiration stored in the token is set to
     *       {@code creationTime + inactivityTimeout} (not the configured {@code expiration}).</li>
     *   <li>On token validation: the stored expiration field is ignored; the effective
     *       expiration is recalculated as {@code creationTime + expiration} from the
     *       server configuration.</li>
     * </ul>
     * <p>
     * This is a beta feature and is only available when running in beta mode.
     *
     * @return {@code true} if dynamic expiration validation is enabled, {@code false} otherwise
     * @ibm-api
     */
    boolean isDynamicExpirationValidation();

    /**
     * @return authFiler reference
     */
    String getAuthFilterRef();

    /**
     * @return Maximum expiration difference allowed
     */
    long getExpirationDifferenceAllowed();

    /**
     * @return monitor interval
     */
    long getMonitorInterval();

    /**
     * @return boolean for monitoring validation keys dir
     */
    boolean getMonitorValidationKeysDir();

    /**
     * @return update trigger
     */
    String getUpdateTrigger();

    /**
     * @return validation Keys
     */
    List<Properties> getValidationKeys();

}
