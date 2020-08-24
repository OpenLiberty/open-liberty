/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Used to ensure a single FAT does not produce too much trace logs.
 */
public class LogPolice {

    private static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    private static final long MB = 1024000L; // bytes in 1MB
    // TODO: incrementally constrict this down to ~300MB trace allowed per bucket
    private static final long MAX_ALLOWED_TRACE = MB * Integer.getInteger("fat.test.max.allowed.trace.mb", 750); // 1GB max, override w/ system prop
    private static long usedTrace = 0;
    private static long effectiveMaxTrace = effectiveMaxTrace(MAX_ALLOWED_TRACE);

    /**
     * Sets the maximum allowed trace for a given FAT bucket. If the limit is exceeded, a test failure will be generated.
     *
     * @param maxTraceInMB The maximum allowed trace (in MB) to be produced by the current FAT
     */
    public static void setMaxAllowedTraceMB(int maxTraceInMB) {
        if (maxTraceInMB > 10000)
            throw new RuntimeException("You know this is max trace in MB (not KB or bytes), right?");
        effectiveMaxTrace = effectiveMaxTrace(maxTraceInMB * MB);
        Log.info(LogPolice.class, "setMaxAllowedTraceMB", "The max allowed trace has been set to " + effectiveMaxTrace / MB +
                                                          "MB.  In LITE mode it will be " + maxTraceInMB +
                                                          "MB, in FULL mode it will be " + maxTraceInMB * 3 +
                                                          ", and 85% of those when running locally.");
    }

    private LogPolice() {
        // static-only class
    }

    public static void measureUsedTrace(long traceFileLength) {
        usedTrace += traceFileLength;
    }

    /**
     * Blows up if the currently running FAT has produced too much trace. See LogPolice.MAX_ALLOWED_TRACE for the current limit.
     */
    static void checkUsedTrace() throws Exception {
        Log.info(LogPolice.class, "checkUsedTrace", "So far this FAT has used " + (usedTrace / MB) + "MB of trace.");

        if (usedTrace > effectiveMaxTrace)
            throw new IllegalStateException("This FAT has used up too much trace!  The maximum allowed trace is " + (effectiveMaxTrace / MB) +
                                            "MB but so far this FAT has logged " + (usedTrace / MB) + "MB of trace.");
    }

    private static long effectiveMaxTrace(long maxAllowedTrace) {
        long effectiveMaxTrace = maxAllowedTrace;
        if (FAT_TEST_LOCALRUN)
            effectiveMaxTrace *= 0.85;
        if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL)
            effectiveMaxTrace *= 3;
        return effectiveMaxTrace;
    }
}
