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
            MINOR = Integer.valueOf(versionElements[i]);
        else
            MINOR = 0;

        String vendor = getSystemProperty("java.vendor").toLowerCase();
        if (vendor.contains("ibm"))
            VENDOR = Vendor.IBM;
        else if (vendor.contains("openj9"))
            VENDOR = Vendor.OPENJ9;
        else if (vendor.contains("oracle"))
            VENDOR = Vendor.ORACLE;
        else
            VENDOR = Vendor.UNKNOWN;

        int sr = 0;
        int fp = 0;

        if (VENDOR == Vendor.IBM) {
            // Parse service release
            String runtimeVersion = getSystemProperty("java.runtime.version").toLowerCase();
            int srloc = runtimeVersion.indexOf("sr");
            if (srloc > (-1)) {
                srloc += 2;
                if (srloc < runtimeVersion.length()) {
                    int len = 0;
                    while ((srloc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(srloc + len))) {
                        len++;
                    }
                    sr = Integer.parseInt(runtimeVersion.substring(srloc, srloc + len));
                }
            }

            // Parse fixpack
            int fploc = runtimeVersion.indexOf("fp");
            if (fploc > (-1)) {
                fploc += 2;
                if (fploc < runtimeVersion.length()) {
                    int len = 0;
                    while ((fploc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(fploc + len))) {
                        len++;
                    }
                    fp = Integer.parseInt(runtimeVersion.substring(fploc, fploc + len));
                }
            }
        }

        SERVICE_RELEASE = sr;
        FIXPACK = fp;

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

    public static Vendor vendor() {
        return instance().VENDOR;
    }

    public static int serviceRelease() {
        return instance().SERVICE_RELEASE;
    }

    public static int fixPack() {
        return instance().FIXPACK;
    }
}
