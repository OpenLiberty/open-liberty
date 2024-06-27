/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.app;

/**
 * A class used for identifying properties of a JDK
 */
public class JavaInfo {
    private static JavaInfo instance;

    public static int JAVA_VERSION = majorVersion();

    private final String JAVA_HOME;
    private final int MAJOR;
    private final int MINOR;
    private final int MICRO;
    private final int SERVICE_RELEASE;
    private final int FIXPACK;

    private JavaInfo() {
        JAVA_HOME = System.getProperty("java.home");

        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.parseInt(versionElements[0]) == 1 ? 1 : 0;
        MAJOR = Integer.parseInt(versionElements[i++]);

        if (i < versionElements.length)
            MINOR = Integer.parseInt(versionElements[i++]);
        else
            MINOR = 0;

        if (i < versionElements.length)
            MICRO = Integer.parseInt(versionElements[i]);
        else
            MICRO = 0;

        // Parse service release
        String buildInfo = System.getProperty("java.runtime.version");
        int sr = 0;
        int srloc = buildInfo.toLowerCase().indexOf("sr");
        if (srloc > (-1)) {
            srloc += 2;
            if (srloc < buildInfo.length()) {
                int len = 0;
                while ((srloc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(srloc + len))) {
                    len++;
                }
                sr = Integer.parseInt(buildInfo.substring(srloc, srloc + len));
            }
        }
        SERVICE_RELEASE = sr;

        // Parse fixpack
        int fp = 0;
        int fploc = buildInfo.toLowerCase().indexOf("fp");
        if (fploc > (-1)) {
            fploc += 2;
            if (fploc < buildInfo.length()) {
                int len = 0;
                while ((fploc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(fploc + len))) {
                    len++;
                }
                fp = Integer.parseInt(buildInfo.substring(fploc, fploc + len));
            }
        }
        FIXPACK = fp;
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

    public static String javaHome() {
        return instance().JAVA_HOME;
    }

    public static int serviceRelease() {
        return instance().SERVICE_RELEASE;
    }

    public static int fixpack() {
        return instance().FIXPACK;
    }

    @Override
    public String toString() {
        return "major=" + MAJOR + ", minor=" + MINOR + ", micro=" + MICRO + ", service release=" + SERVICE_RELEASE
               + ", fixpack=" + FIXPACK + ", javaHome=" + JAVA_HOME;
    }
}
