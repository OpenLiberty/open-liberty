/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Utility 'helper' class to get the singleton {@link LoggedOutCookieCache}
 * instance.
 */
public class LoggedOutCookieCacheHelper {

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(LoggedOutCookieCacheHelper.class, "LoggedOutCookieCache");

    private static LoggedOutCookieCache cookieCacheService = null;

    /**
     * Get the singleton {@link LoggedOutCookieCache} instance.
     *
     * @return The {@link LoggedOutCookieCache} instance, or null if one was not
     *         set.
     */
    public static LoggedOutCookieCache getLoggedOutCookieCacheService() {
        return cookieCacheService;
    }

    /**
     * Set the singleton {@link LoggedOutCookieCache} instance.
     *
     * @param The {@link LoggedOutCookieCache} instance, or null to unset.
     */
    public static void setLoggedOutCookieCacheService(LoggedOutCookieCache service) {
        cookieCacheService = service;
    }
}
