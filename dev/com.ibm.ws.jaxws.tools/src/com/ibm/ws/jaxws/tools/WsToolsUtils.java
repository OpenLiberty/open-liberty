/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.tools;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import com.ibm.ws.jaxws.tools.internal.JaxWsToolsConstants;

/**
 * Collected methods used by WsGen and WsImport in a utility class*
 */
class WsToolsUtils {

    static String getJarFileOfClass(final Class<?> javaClass) {
        ProtectionDomain protectionDomain = AccessController.doPrivileged(
                                                                          new PrivilegedAction<ProtectionDomain>() {
                                                                              @Override
                                                                              public ProtectionDomain run() {
                                                                                  return javaClass.getProtectionDomain();
                                                                              }
                                                                          });

        String path = protectionDomain.getCodeSource().getLocation().getPath();
        if (isWindows()) {
            // If it's Windows and starts with '/' remove '/'
            path = path.substring(1);
        }
        return path;
    }

    private static boolean isWindows() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("win"));
    }

    static int getMajorJavaVersion() {
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D");
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        return Integer.valueOf(versionElements[i]);
    }

    static boolean isTargetRequired(String[] args) {
        boolean helpExisted = false;
        boolean versionExisted = false;
        boolean targetExisted = false;

        for (String arg : args) {
            if (arg.equals(JaxWsToolsConstants.PARAM_HELP)) {
                helpExisted = true;
            } else if (arg.equals(JaxWsToolsConstants.PARAM_VERSION)) {
                versionExisted = true;
            } else if (arg.equals(JaxWsToolsConstants.PARAM_TARGET)) {
                targetExisted = true;
            }

            continue;
        }

        return args.length > 0 && !helpExisted && !versionExisted && !targetExisted;
    }
}
