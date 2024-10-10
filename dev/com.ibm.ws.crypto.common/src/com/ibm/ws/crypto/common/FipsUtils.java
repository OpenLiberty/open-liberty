/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.common;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

public class FipsUtils {

    public static boolean fipsEnabled = false;
    public static boolean fipsChecked = false;
    private static boolean issuedBetaMessage = false;

    private static final TraceComponent tc = Tr.register(FipsUtils.class);

    static String FIPSLevel = getFipsLevel();

    public static String getProperty(final String prop, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(prop, defaultValue).toLowerCase();
            }
        });
    }

    static String getFipsLevel() {
        return getProperty("com.ibm.fips.mode", "disabled");
    }

    public static boolean isSemeruFips() {
        return "true".equals(getProperty("semeru.fips", "false"));
    }

    public static boolean isFips140_3Enabled() {

        return isRunningBetaMode() &&
               "140-3".equals(FIPSLevel) || "true".equalsIgnoreCase(getProperty("global.fips_140-3", "false")) || isSemeruFips();
    }

    public static boolean isFips140_2Enabled() {
        return isRunningBetaMode() && "140-2".equals(FIPSLevel);
    }

    public static boolean isFIPSEnabled() {
        if (fipsChecked) {
            return fipsEnabled;
        } else {
            fipsEnabled = isFips140_2Enabled() || isFips140_3Enabled();
            fipsChecked = true;
            return fipsEnabled;
        }
    }

    public static boolean isRunningBetaMode() {

        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for
            // this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class FipsUtils for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
    }
}
