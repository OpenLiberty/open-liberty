/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.lang.management.ManagementFactory;
import java.util.List;

import com.ibm.ws.kernel.service.util.JavaInfo;

/**
 * Checks whether the JVM was started with native access enabled for classpath
 * code (unnamed module). Used to detect when Liberty-managed native libraries
 * will trigger JEP 472 warnings or failures on Java 24+.
 *
 * <p>The result is computed once from the JVM launch arguments and cached.
 * All methods are safe to call from any thread.
 */
class NativeLibraryAccessChecker {

    /** Cached result. null = not yet computed. */
    private static volatile Boolean nativeAccessEnabled = null;

    /** Static utility class - no instances. */
    private NativeLibraryAccessChecker() {}

    /**
     * Returns true if native access is enabled for the unnamed module, or if
     * we are running on Java older than 24 (where no restriction exists).
     * Returns true on any error (fail open - never block the library load).
     */
    static boolean isNativeAccessEnabled() {
        if (JavaInfo.majorVersion() < 24) {
            return true;
        }
        Boolean cached = nativeAccessEnabled;
        if (cached != null) {
            return cached;
        }
        boolean result = computeNativeAccessEnabled();
        nativeAccessEnabled = result;
        return result;
    }

    private static boolean computeNativeAccessEnabled() {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            return scanArgs(args);
        } catch (Throwable t) {
            // If we cannot determine the answer, assume native access is enabled
            // so that we do not spam warnings for scenarios we do not understand.
            return true;
        }
    }

    private static boolean scanArgs(List<String> args) {
        try {
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);

                // Old flag form that silences all native-access restrictions.
                if ("--illegal-native-access=allow".equals(arg)) {
                    return true;
                }

                // --enable-native-access=ALL-UNNAMED
                // --enable-native-access=java.base,ALL-UNNAMED  (comma list)
                if (arg.startsWith("--enable-native-access=")) {
                    String value = arg.substring("--enable-native-access=".length());
                    if (containsAllUnnamed(value)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Returns true if the comma-separated module list value contains ALL-UNNAMED.
     * For example "java.base,ALL-UNNAMED" returns true.
     */
    private static boolean containsAllUnnamed(String value) {
        String[] parts = value.split(",");
        for (int i = 0; i < parts.length; i++) {
            if ("ALL-UNNAMED".equals(parts[i].trim())) {
                return true;
            }
        }
        return false;
    }
}
