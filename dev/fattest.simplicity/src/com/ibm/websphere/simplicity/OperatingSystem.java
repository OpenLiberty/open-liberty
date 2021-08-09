/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Represents the operating system of a local or remote device.
 * 
 * Overrides the simplicity version and is needed to include MAC and other OS'
 */
public enum OperatingSystem {

    AIX,
    HP,
    LINUX,
    ISERIES,
    MAC,
    SOLARIS,
    WINDOWS,
    ZOS;

    private static final Class<?> c = OperatingSystem.class;

    private static final String FILE_ENCODING_DEFAULT = "ISO8859-1";
    private static final String FILE_ENCODING_Z = "cp1047";
    private static final String FILE_SUFFIX_DEFAULT = ".sh";
    private static final String FILE_SUFFIX_WINDOWS = ".bat";
    private static final String FILE_SUFFIX_I = "";
    private static final String FILE_SEPARATOR_DEFAULT = "/";
    private static final String FILE_SEPARATOR_WINDOWS = "\\";
    private static final String PATH_SEPARATOR_DEFAULT = ":";
    private static final String PATH_SEPARATOR_WINDOWS = ";";
    private static final String LINE_SEPARATOR_DEFAULT = "\n";
    private static final String LINE_SEPARATOR_WINDOWS = "\r\n";
    private static final String ENV_VAR_SET_DEFAULT = "export";
    private static final String ENV_VAR_SET_WINDOWS = "set";

    /**
     * Get the file separator used by this operating system
     * 
     * @return A file separator
     */
    public String getFileSeparator() {
        return (this.equals(WINDOWS) ? FILE_SEPARATOR_WINDOWS : FILE_SEPARATOR_DEFAULT);
    }

    /**
     * Get the path separator used by this operating system
     * 
     * @return A path separator
     */
    public String getPathSeparator() {
        return (this.equals(WINDOWS) ? PATH_SEPARATOR_WINDOWS : PATH_SEPARATOR_DEFAULT);
    }

    /**
     * Get the line separator used by this operating system
     * 
     * @return A line separator
     */
    public String getLineSeparator() {
        return (this.equals(WINDOWS) ? LINE_SEPARATOR_WINDOWS : LINE_SEPARATOR_DEFAULT);
    }

    /**
     * The default file encoding used by this operating system.
     * 
     * @return a file encoding
     */
    public String getDefaultEncoding() {
        return (this.equals(ZOS) ? FILE_ENCODING_Z : FILE_ENCODING_DEFAULT);
    }

    /**
     * @return The shell command to set an environment variable.
     */
    public String getEnvVarSet() {
        return (this.equals(WINDOWS) ? ENV_VAR_SET_WINDOWS : ENV_VAR_SET_DEFAULT);
    }

    /**
     * The default executable file extension used by this operating
     * system
     * 
     * @return ".bat" for Windows, "" for i-Series, ".sh" for everything else
     */
    public String getDefaultScriptSuffix() {
        switch (this) {
            case WINDOWS:
                return FILE_SUFFIX_WINDOWS;
            case ISERIES:
                return FILE_SUFFIX_I;
            default:
                return FILE_SUFFIX_DEFAULT;
        }
    }

    /**
     * Convert an operating system name <code>String</code> to an {@link OperatingSystem} enum.
     * Value values are those returned by {@link OSName#getOSName()}
     * 
     * @param osName The name of the operating system
     * @return The corresponding {@link OperatingSystem}
     * @throws Exception
     */
    public static OperatingSystem getOperatingSystem(String osName) throws Exception {
        final String method = "getOperatingSystem";
        Log.entering(c, method, osName);
        OperatingSystem os = null;

        //We search MAC first beacause mac uses a kernel called "Darwin" and if we were to put windows first
        //while searching for "win" it would get confused and think a mac was a windows...
        if (osName.toLowerCase().indexOf(OSName.OS_NAME_MAC.getOSName().toLowerCase()) != -1
            || osName.toLowerCase().indexOf(OSName.OS_NAME_MAC2.getOSName().toLowerCase()) != -1) {
            os = OperatingSystem.MAC;
        } else if (osName.toLowerCase().indexOf(OSName.OS_NAME_WINDOWS.getOSName().toLowerCase()) != -1) {
            os = OperatingSystem.WINDOWS;
        } else if ((osName.toLowerCase().indexOf(OSName.OS_NAME_ISERIES.getOSName().toLowerCase()) != -1)) {
            os = OperatingSystem.ISERIES;
        } else if ((osName.toLowerCase().indexOf(OSName.OS_NAME_ZOS.getOSName().toLowerCase()) != -1)
                   || (osName.toLowerCase().indexOf(OSName.OS_NAME_ZOS_2.getOSName().toLowerCase()) != -1)
                   || (osName.toLowerCase().indexOf(OSName.OS_NAME_ZOS_3.getOSName().toLowerCase()) != -1)) {
            os = OperatingSystem.ZOS;
        } else if (osName.toLowerCase().indexOf(OSName.OS_NAME_LINUX.getOSName().toLowerCase()) != -1) {
            os = OperatingSystem.LINUX;
        } else if (osName.toLowerCase().indexOf(OSName.OS_NAME_HP.getOSName().toLowerCase()) != -1) {
            os = OperatingSystem.HP;
        } else if ((osName.toLowerCase().indexOf(OSName.OS_NAME_SOLARIS.getOSName().toLowerCase()) != -1)
                   || (osName.toLowerCase().indexOf(OSName.OS_NAME_SOLARIS_2.getOSName().toLowerCase()) != -1)) {
            os = OperatingSystem.SOLARIS;
        } else if (osName.toLowerCase().indexOf(OSName.OS_NAME_AIX.getOSName().toLowerCase()) != -1) {
            os = OperatingSystem.AIX;
        } else {
            throw new Exception("Unknown OS name: " + osName);
        }
        Log.exiting(c, method, os);
        return os;
    }

    /**
     * This enum contains known operating system names
     */
    public enum OSName {
        OS_NAME_WINDOWS("Win"),
        OS_NAME_ISERIES("400"),
        OS_NAME_ZOS("OS/390"),
        OS_NAME_ZOS_2("z/OS"),
        OS_NAME_ZOS_3("zOS"),
        OS_NAME_LINUX("Linux"),
        OS_NAME_HP("HP"),
        OS_NAME_MAC("Darwin"),
        OS_NAME_MAC2("Mac OS"),
        OS_NAME_SOLARIS("Solaris"),
        OS_NAME_SOLARIS_2("Sun"),
        OS_NAME_AIX("AIX");

        private String name;

        private OSName(String name) {
            this.name = name;
        }

        public String getOSName() {
            return this.name;
        }
    }
}
