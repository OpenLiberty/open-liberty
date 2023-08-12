/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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
    public static final String CFG_KEY_PASSWORD = "keysPassword";

    /**
     * The token expiration.
     */
    public static final String CFG_KEY_TOKEN_EXPIRATION = "expiration";

    /**
     * The token keys file's monitor interval.
     */
    public static final String CFG_KEY_MONITOR_INTERVAL = "monitorInterval";

    /**
     * The Boolean to monitor the token keys file's directory.
     */
    static final String CFG_KEY_MONITOR_DIRECTORY = "monitorDirectory";

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
    static final String CFG_KEY_VALIDATION_NOT_USE_AFTER_DATE = "notUseAfterDate";

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
     * @return LTPA expiration
     */
    long getTokenExpiration();

    /**
     * @return authFiler reference
     */
    String getAuthFilterRef();

    /**
     * @return Maximum expiration difference allowed
     */
    long getExpirationDifferenceAllowed();

    /**
     * @return monitor directory
     */
    boolean getMonitorDirectory();

    /**
     * @return validation Keys
     */
    List<Properties> getValidationKeys();

}
