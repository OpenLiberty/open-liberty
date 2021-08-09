/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.application.handler.ApplicationHandler;

/**
 * An applicationHandler can provide this interface to replace messages logged about the application.
 * For example, resource adapter "applications" should have messages that say
 * "the resource adapter was installed"
 * instead of
 * "the application was started".
 */
@Trivial
public class AppMessageHelper {
    private static final TraceComponent tc = Tr.register(AppMessageHelper.class);

    private static final AppMessageHelper defaultInstance = new AppMessageHelper();

    /**
     * Log an audit message.
     * 
     * @param key message key for the application manager messages file.
     * @param params message parameters.
     */
    public void audit(String key, Object... params) {
        Tr.audit(tc, key, params);
    }

    /**
     * Format a message.
     * 
     * @param key message key for the application manager messages file.
     * @param params message parameters.
     * @return the translated message.
     */
    public String formatMessage(String key, Object... params) {
        return Tr.formatMessage(tc, key, params);
    }

    /**
     * Log an error message.
     * 
     * @param key message key for the application manager messages file.
     * @param params message parameters.
     */
    public void error(String key, Object... params) {
        Tr.error(tc, key, params);
    }

    /**
     * Retrieves the message helper for the specified application handler.
     * If there isn't any, then returns the default message helper for app manager.
     * 
     * @param handler application handler for which to obtain the corresponding message helper.
     * @return message helper.
     */
    public static final AppMessageHelper get(ApplicationHandler<?> handler) {
        return handler instanceof AppMessageHelper ? (AppMessageHelper) handler : defaultInstance;
    }

    /**
     * Log an informational message.
     * 
     * @param key message key for the application manager messages file.
     * @param params message parameters.
     */
    public void info(String key, Object... params) {
        Tr.info(tc, key, params);
    }

    /**
     * Log a warning message.
     * 
     * @param key message key for the application manager messages file.
     * @param params message parameters.
     */
    public void warning(String key, Object... params) {
        Tr.warning(tc, key, params);
    }
}