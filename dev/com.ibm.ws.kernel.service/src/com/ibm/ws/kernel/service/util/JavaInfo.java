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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

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

    private static final Map<String, Boolean> systemClassAvailability = new ConcurrentHashMap<>();

    /**
     * In rare cases where different behaviour is performed based on the JVM vendor
     * this method should be used to test for a unique JVM class provided by the
     * vendor rather than using the vendor method. For example if on JVM provides a
     * different Kerberos login module testing for that login module being loadable
     * before configuring to use it is preferable to using the vendor data.
     *
     * New users of this method should consider adding their class name in
     * JavaInfoTest in the com.ibm.ws.java11_fat project.
     *
     * @param className the name of a class in the JVM to test for
     * @return true if the class is available, false otherwise.
     */
    public static boolean isSystemClassAvailable(String className) {
        return systemClassAvailability.computeIfAbsent(className, (k) -> AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            @FFDCIgnore(ClassNotFoundException.class)
            public Boolean run() {
                try {
                    // Using ClassLoader.findSystemClass() instead of
                    // Class.forName(className, false, null) because Class.forName with a null
                    // ClassLoader only looks at the boot ClassLoader with Java 9 and above
                    // which doesn't look at all the modules available to the findSystemClass.
                    systemClassAccessor.getSystemClass(className);
                    return true;
                } catch (ClassNotFoundException e) {
                    //No FFDC needed
                    return false;
                }
            }
        }));
    }

    private static final SystemClassAccessor systemClassAccessor = new SystemClassAccessor();

    private static final class SystemClassAccessor extends ClassLoader {
        public Class<?> getSystemClass(String className) throws ClassNotFoundException {
            return findSystemClass(className);
        }
    }

    @Deprecated
    /**
     * This method should not be used to change behaviour based on the Java vendor.
     * Instead if there are behaviour differences between JVMs a test should be performed
     * to detect the actual capability used before making a decision. For example if there
     * is a different class on one JVM that needs to be used vs another an attempt should
     * be made to load the class and take the code path.
     *
     * <p>This method is intended to only be used for debug purposes.</p>
     *
     * @return the detected vendor of the JVM
     */
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
     * For debug purposes only
     *
     * @return a String containing basic info about the JDK
     */
    public static String debugString() {
        return "Vendor = " + vendor() + ", Version = " + majorVersion() + "." + minorVersion();
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
