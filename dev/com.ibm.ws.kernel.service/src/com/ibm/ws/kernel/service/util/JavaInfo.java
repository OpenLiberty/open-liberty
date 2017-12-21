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
    private final Vendor VENDOR;

    private JavaInfo() {
        String version = PrivHelper.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        MAJOR = Integer.valueOf(versionElements[i++]);

        if (i < versionElements.length)
            MINOR = Integer.valueOf(versionElements[i]);
        else
            MINOR = 0;

        String vendor = PrivHelper.getProperty("java.vendor").toLowerCase();
        if (vendor.contains("ibm"))
            VENDOR = Vendor.IBM;
        else if (vendor.contains("openj9"))
            VENDOR = Vendor.OPENJ9;
        else if (vendor.contains("oracle"))
            VENDOR = Vendor.ORACLE;
        else
            VENDOR = Vendor.UNKNOWN;
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
}
