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

import java.security.Security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.JavaInfo;

public class CryptoProvider {
    private static final TraceComponent tc = Tr.register(CryptoProvider.class);

    public static boolean ibmJCEAvailable = false;
    public static boolean ibmJCEPlusFIPSAvailable = false;
    public static boolean openJCEPlusAvailable = false;
    public static boolean openJCEPlusFIPSAvailable = false;
    public static boolean ibmJCEProviderChecked = false;
    public static boolean ibmJCEPlusFIPSProviderChecked = false;
    public static boolean openJCEPlusProviderChecked = false;
    public static boolean openJCEPlusFIPSProviderChecked = false;

    public static boolean unitTest = false;
    public static boolean fipsEnabled = FipsUtils.isFIPSEnabled();
    public static boolean fipsChecked = false;

    public static boolean javaVersionChecked = false;
    public static boolean isJava11orHigher = false;

    public static boolean zOSAndJAVA11orHigherChecked = false;
    public static boolean iszOSAndJava11orHigher = false;

    public static String osName = System.getProperty("os.name");
    public static boolean isZOS = false;
    public static boolean osVersionChecked = false;

    public static String IBMJCE_PROVIDER = "com.ibm.crypto.provider.IBMJCE";
    public static String IBMJCE_PLUS_FIPS_PROVIDER = "com.ibm.crypto.provider.IBMJCEPlusFIPS";
    public static String OPENJCE_PLUS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlus";
    public static String OPENJCE_PLUS_FIPS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlusFIPS";

    public static final String IBMJCE_NAME = "IBMJCE";
    public static final String IBMJCE_PLUS_FIPS_NAME = "IBMJCEPlusFIPS";
    public static final String OPENJCE_PLUS_NAME = "OpenJCEPlus";
    public static final String OPENJCE_PLUS_FIPS_NAME = "OpenJCEPlusFIPS";

    private static boolean issuedBetaMessage = false;

    public static boolean isIBMJCEAvailable() {
        if (ibmJCEProviderChecked) {
            return ibmJCEAvailable;
        } else {
            ibmJCEAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PROVIDER);
            ibmJCEProviderChecked = true;
            return ibmJCEAvailable;
        }
    }

    public static boolean isIBMJCEPlusFIPSAvailable() {
        if (ibmJCEPlusFIPSProviderChecked) {
            return ibmJCEPlusFIPSAvailable;
        } else {
            ibmJCEPlusFIPSAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PLUS_FIPS_PROVIDER);
            ibmJCEPlusFIPSProviderChecked = true;

            if (FipsUtils.isRunningBetaMode() && ibmJCEPlusFIPSAvailable) {
                ibmJCEPlusFIPSAvailable = true;
            } else {
                if (fipsEnabled) {
                    Tr.error(tc, "FIPS is enabled but the IBMJCEPlusFIPS provider is not available.");
                }
                ibmJCEPlusFIPSAvailable = false;
            }
            return ibmJCEPlusFIPSAvailable;
        }
    }

    public static boolean isOpenJCEPlusAvailable() {
        if (openJCEPlusProviderChecked) {
            return openJCEPlusAvailable;
        } else {
            openJCEPlusAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_PROVIDER);
            openJCEPlusProviderChecked = true;
            return openJCEPlusAvailable;
        }
    }

    public static boolean isOpenJCEPlusFIPSAvailable() {
        if (openJCEPlusFIPSProviderChecked) {
            return openJCEPlusFIPSAvailable;
        } else {
            openJCEPlusFIPSAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_FIPS_PROVIDER);
            openJCEPlusFIPSProviderChecked = true;

            if (FipsUtils.isRunningBetaMode() && openJCEPlusFIPSAvailable) {
                openJCEPlusFIPSAvailable = true;
            } else {
                if (fipsEnabled) {
                    Tr.error(tc, "FIPS is enabled but the OpenJCEPlusFIPS provider is not available.");
                }
                openJCEPlusFIPSAvailable = false;
            }
            return openJCEPlusFIPSAvailable;
        }
    }

    private static boolean isJava11orHigher() {
        if (javaVersionChecked) {
            return isJava11orHigher;
        } else {
            isJava11orHigher = JavaInfo.majorVersion() >= 11;
            javaVersionChecked = true;
            return isJava11orHigher;
        }
    }

    private static boolean isZOS() {
        if (osVersionChecked) {
            return isZOS;
        } else {
            isZOS = (osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390"));
            osVersionChecked = true;
            return isZOS;
        }
    }

    public static boolean isZOSandRunningJava11orHigher() {
        if (zOSAndJAVA11orHigherChecked) {
            return iszOSAndJava11orHigher;
        } else {
            iszOSAndJava11orHigher = isJava11orHigher() && isZOS();
            zOSAndJAVA11orHigherChecked = true;
            return iszOSAndJava11orHigher;
        }
    }

    public static String getProvider() {
        String provider = null;
        if (fipsEnabled && CryptoProvider.isOpenJCEPlusFIPSAvailable()) {
            provider = OPENJCE_PLUS_FIPS_NAME;
        } else if (fipsEnabled && CryptoProvider.isIBMJCEPlusFIPSAvailable()) {
            provider = IBMJCE_PLUS_FIPS_NAME;
        } else if (CryptoProvider.isZOSandRunningJava11orHigher() && CryptoProvider.isOpenJCEPlusAvailable()) {
            provider = OPENJCE_PLUS_NAME;
        } else if (CryptoProvider.isIBMJCEAvailable()) {
            provider = IBMJCE_NAME;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (provider == null) {
                Tr.debug(tc, "getProvider" + " Provider configured by JDK is " + Security.getProviders()[0].getName());
            } else {
                Tr.debug(tc, "getProvider" + " Provider configured is " + provider);
            }
        }
        return provider;
    }
}
