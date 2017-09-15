/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract.platform;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 *
 */
public class Platform {

    private static final String S_AIX_PATTERN = "^.*aix.*$";
    private static final String S_HPUX_PATTERN = "^.*hp-ux.*$";
    private static final String S_SOLARIS_PATTERN = "^.*sunos.*$";
    private static final String S_LINUX_PATTERN = "^.*linux.*$";
    private static final String S_WINDOWS_PATTERN = "^.*windows.*$";
    private static final String S_Z_OS_PATTERN = "^.*z/os.*$";
    private static final String S_OS400_PATTERN = "^.*os/400.*$";
    private static final String S_MAC_OS_PATTERN = "^.*mac.*$";

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    public static boolean isWindows() {
        return Pattern.matches(S_WINDOWS_PATTERN, OS_NAME);
    }

    public static boolean isLinux() {
        return Pattern.matches(S_LINUX_PATTERN, OS_NAME);
    }

    public static boolean isAIX() {
        return Pattern.matches(S_AIX_PATTERN, OS_NAME);
    }

    public static boolean isHPUX() {
        return Pattern.matches(S_HPUX_PATTERN, OS_NAME);
    }

    public static boolean isMACOS() {
        return Pattern.matches(S_MAC_OS_PATTERN, OS_NAME);
    }

    public static boolean isOS400() {
        return Pattern.matches(S_OS400_PATTERN, OS_NAME);
    }

    public static boolean isSolaris() {
        return Pattern.matches(S_SOLARIS_PATTERN, OS_NAME);
    }

    public static boolean isZOS() {
        return Pattern.matches(S_Z_OS_PATTERN, OS_NAME);
    }

}
