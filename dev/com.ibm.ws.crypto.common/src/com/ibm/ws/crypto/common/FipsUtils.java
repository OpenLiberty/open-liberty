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

    private static final TraceComponent tc = Tr.register(FipsUtils.class);

    static String FIPSLevel = getFipsLevel();

    static String getFipsLevel() {
        String fipsLevel = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String propertyValue = System.getProperty("com.ibm.fips.mode");
                return (propertyValue == null) ? "disabled" : propertyValue.trim().toLowerCase();
            }
        });
        return fipsLevel;
    }

    public static boolean isFips140_3Enabled() {
        return isRunningBetaMode() && "140-3".equals(FIPSLevel);
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
        return ProductInfo.getBetaEdition();
    }

}
