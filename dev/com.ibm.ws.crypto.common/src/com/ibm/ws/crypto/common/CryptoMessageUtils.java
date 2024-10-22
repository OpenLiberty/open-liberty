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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo; // TODO remove beta check

public class CryptoMessageUtils {

    private static TraceComponent tc = Tr.register(CryptoMessageUtils.class);

    
    public static void logInsecureAlgorithm(String configProperty, String insecureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE", configProperty, insecureAlgorithm);
        }
    }

    public static void logInsecureAlgorithmReplaced(String configProperty, String insecureAlgorithm, String secureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_REPLACED", configProperty, insecureAlgorithm, secureAlgorithm);
        }
    }

    public static void logInsecureProvider(String provider, String insecureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_PROVIDER", provider, insecureAlgorithm);
        }
    }
    
    // TODO remove beta check
    static boolean isRunningBetaMode() {
        return ProductInfo.getBetaEdition();
    }
}
