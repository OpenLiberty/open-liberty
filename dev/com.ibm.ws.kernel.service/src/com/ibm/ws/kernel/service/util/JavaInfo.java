/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * API for reading information related to the JDK
 */
public class JavaInfo {

    public static enum Vendor {
        IBM,
        OPENJ9,
        ORACLE,
        UNKNOWN
    }

    private static JavaInfo instance;

    private final int MAJOR;
    private final int MINOR;
    private final int MICRO;
    private final int SERVICE_RELEASE;
    private final int FIXPACK;
    private final Vendor VENDOR;

    private JavaInfo() {
        String version = getSystemProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        MAJOR = Integer.valueOf(versionElements[i++]);

        if (i < versionElements.length)
            MINOR = parseIntSafe(versionElements[i++]);
        else
            MINOR = 0;

        if (i < versionElements.length)
            MICRO = parseIntSafe(versionElements[i]);
        else
            MICRO = 0;

        String vendor = getSystemProperty("java.vendor").toLowerCase();
        if (vendor.contains("openj9"))
            VENDOR = Vendor.OPENJ9;
        else if (vendor.contains("ibm") || vendor.contains("j9"))
            VENDOR = Vendor.IBM;
        else if (vendor.contains("oracle"))
            VENDOR = Vendor.ORACLE;
        else {
            vendor = getSystemProperty("java.vm.name", "unknown").toLowerCase();
            if (vendor.contains("openj9"))
                VENDOR = Vendor.OPENJ9;
            else if (vendor.contains("ibm") || vendor.contains("j9"))
                VENDOR = Vendor.IBM;
            else if (vendor.contains("oracle") || vendor.contains("openjdk"))
                VENDOR = Vendor.ORACLE;
            else
                VENDOR = Vendor.UNKNOWN;
        }

        String runtimeVersion = getSystemProperty("java.runtime.version").toLowerCase();

        // Parse service release
        int sr = 0;
        int srloc = runtimeVersion.indexOf("sr");
        if (srloc > (-1)) {
            srloc += 2;
            if (srloc < runtimeVersion.length()) {
                int len = 0;
                while ((srloc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(srloc + len))) {
                    len++;
                }
                sr = parseIntSafe(runtimeVersion.substring(srloc, srloc + len));
            }
        }
        SERVICE_RELEASE = sr;

        // Parse fixpack
        int fp = 0;
        int fploc = runtimeVersion.indexOf("fp");
        if (fploc > (-1)) {
            fploc += 2;
            if (fploc < runtimeVersion.length()) {
                int len = 0;
                while ((fploc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(fploc + len))) {
                    len++;
                }
                fp = parseIntSafe(runtimeVersion.substring(fploc, fploc + len));
            }
        }
        FIXPACK = fp;
    }

    private static final String getSystemProperty(final String propName, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propName, defaultValue);
            }
        });
    }

    private static final String getSystemProperty(final String propName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propName);
            }
        });
    }

    private static JavaInfo instance() {
        if (instance == null)
            instance = new JavaInfo();
        return instance;
    }

    public static int majorVersion() {
        return instance().MAJOR;
    }

    public static int minorVersion() {
        return instance().MINOR;
    }

    public static int microVersion() {
        return instance().MICRO;
    }

    public static Vendor vendor() {
        return instance().VENDOR;
    }

    public static int serviceRelease() {
        return instance().SERVICE_RELEASE;
    }

    public static int fixPack() {
        return instance().FIXPACK;
    }

    /**
     * @return the integer value of the string, or 0 if the string cannot be coerced to a string
     */
    private static int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
