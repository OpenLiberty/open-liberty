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
package com.ibm.ws.kernel.boot.cmdline;

import java.util.ResourceBundle;

/**
 * Check's the version of the Java running before starting the server or running commands,
 * if Java 7 (or below) is being used a translated error message is thrown.
 */
public class Main {

    private static final int CLASS_MAJOR_VERSION_JAVA8 = 52;

    // See Launcher.ReturnCode.
    private static final int ERROR_BAD_JAVA_VERSION = 30;

    // See Launcher.ReturnCode.
    private static final int ERROR_BAD_JAVA_BITMODE = 31;

    protected static void invokeMain(String[] args) {}

    /**
     * @param args - will just get passed onto Launcher if version check is successful
     */
    public static void main(String[] args) {
        // gij (GNU libgcj) is installed as "java" on some Linux machines.
        // This interpreter really only supports Java 5, but unhelpfully fails
        // to throw UnsupportedClassVersionError for Java 6 classes.
        if (getClassMajorVersion() < CLASS_MAJOR_VERSION_JAVA8) {
            badVersion();
        }

        // On z/OS only 64-bit Java is supported  
        String osName = System.getProperty("os.name");
        if (osName.equals("z/OS")) {
            String bitmode = System.getProperty("com.ibm.vm.bitmode");
            if (bitmode.equals("32"))
                badBitMode();
        }

        try {
            com.ibm.ws.kernel.boot.cmdline.UtilityMain.main(args);

            // if we actually get back here, make sure we do exit with a return code
            // and not just a cessation of output. The tools look for a return code value.
            System.exit(0);
        } catch (UnsupportedClassVersionError versionError) {
            badVersion();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR_BAD_JAVA_VERSION);
        }
    }

    private static void badVersion() {
        System.out.println(ResourceBundle.getBundle("com.ibm.ws.jaxrs20.tools.internal.resources.JaxRsToolsMessages").getString("error.badVersion"));
        System.exit(ERROR_BAD_JAVA_VERSION);
    }

    private static void badBitMode() {
        System.out.println(ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages").getString("error.badBitmode"));
        System.exit(ERROR_BAD_JAVA_BITMODE);
    }

    private static int getClassMajorVersion() {
        String classVersion = System.getProperty("java.class.version");
        if (classVersion == null) {
            // JVM didn't supply the system property.  Let's hope it throws
            // UnsupportedClassVersionError if necessary.
            return CLASS_MAJOR_VERSION_JAVA8;
        }

        int index = classVersion.indexOf('.');
        String majorVersion = index == -1 ? classVersion : classVersion.substring(0, index);

        try {
            return Integer.parseInt(majorVersion);
        } catch (NumberFormatException ex) {
            // We couldn't parse the version string.  Let's hope the JVM throws
            // UnsupportedClassVersionError if necessary.
            return CLASS_MAJOR_VERSION_JAVA8;
        }
    }
}
