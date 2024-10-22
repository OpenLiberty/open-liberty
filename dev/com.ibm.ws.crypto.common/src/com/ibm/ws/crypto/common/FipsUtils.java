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

    private static final TraceComponent tc = Tr.register(FipsUtils.class);

    static String FIPSLevel = getFipsLevel();

    //TODO remove with beta checks
    static boolean unitTest = false;

    static String getFipsLevel() {
        String fipsLevel = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("com.ibm.fips.mode");
            }
        });
        return fipsLevel;
    }

    public static boolean isFips140_3Enabled() {
        //TODO remove beta check
        if (unitTest) {
            return "140-3".equals(FIPSLevel);
        } else {
            return isRunningBetaMode() && "140-3".equals(FIPSLevel);
        }
    }

    //TODO remove beta check
    static boolean isRunningBetaMode() {
        return ProductInfo.getBetaEdition();
    }

}
