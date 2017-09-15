/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.Locale;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Header: IBM-App-ForceRestart
 * <p>
 * Description: Causes applications to be restarted when the feature is installed to or uninstalled from a running server.
 * <p>
 * Possible values are: 'install', 'uninstall' or 'install,uninstall' which indicate
 * which operation(s) will cause application restarts.
 * 
 * Required: No
 */
public enum AppForceRestart {
    /** Force applications to be restarted when this feature is installed */
    INSTALL,

    /** Force applications to be restarted when this feature is uninstalled */
    UNINSTALL,

    /** Force applications to be restarted when this feature is installed or uninstalled */
    ALWAYS,

    /** Do nothing */
    NEVER;

    @FFDCIgnore(IllegalArgumentException.class)
    public static AppForceRestart fromString(String s) {
        if (s == null || s.isEmpty())
            return NEVER;

        // start with never.. 
        AppForceRestart result = NEVER;
        try {
            String[] values = s.split(",");
            for (String part : values) {
                AppForceRestart r1 = AppForceRestart.valueOf(part.trim().toUpperCase(Locale.ENGLISH));
                if (result == NEVER)
                    result = r1; // was NEVER, now is either install or uninstall
                else
                    result = ALWAYS; // both install AND uninstall
            }
        } catch (IllegalArgumentException iae) {
        }
        return result;
    }

    public boolean matches(AppForceRestart expectedValue) {
        switch (expectedValue) {
            case INSTALL:
                return this == INSTALL || this == ALWAYS;
            case UNINSTALL:
                return this == UNINSTALL || this == ALWAYS;
            case ALWAYS:
                return this == ALWAYS;
            case NEVER:
                return this == NEVER;
        }
        return false;
    }
}
