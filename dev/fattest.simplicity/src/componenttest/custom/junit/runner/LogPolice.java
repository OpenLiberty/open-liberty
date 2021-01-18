/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
 * Helper which is used to detect when a test class has produced
 * too much trace output. 
 */
public class LogPolice {
    private static final Class<? extends LogPolice> c = LogPolice.class;

    /**
     * Private constructor.  This class is an effective singleton:
     * All state and operations are static.
     */
    private LogPolice() {
        // Disallowed: Static singleton
    }

    private static final long MB = 1024000L;

    /**
     * Adjust a trace measurement based on whether tests are
     * running locally ({@link FATRunner#FAT_TEST_LOCALRUN}),
     * and based on whether the test mode is {@link TestMode#FULL}.
     * 
     * When running locally, reduce the value by 15%.
     * 
     * When running in {@link TestMode#FULL} mode, triple the value.
     * 
     * Both adjustments may be made, and are multiplicative.
     *
     * @param maxAllowedTrace A trace size which is to be adjusted.
     *
     * @return The adjusted trace size.
     */
    private static long effectiveMaxTrace(long maxAllowedTrace) {
        if ( FATRunner.FAT_TEST_LOCALRUN ) {
            maxAllowedTrace *= 0.85;
        }
        if ( TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL ) {
            maxAllowedTrace *= 3;
        }
        return maxAllowedTrace;
    }    
    
    /**
     * Property used to set the maximum allowed trace.  The value is
     * a count of megabytes.
     */
    public static final String MAX_ALLOWED_TRACE_PROPERTY_NAME =
        "fat.test.max.allowed.trace.mb";

    // TODO: Start out with 4GB cap on trace in LITE mode,
    // incrementally reduce this to ~300MB trace per bucket.

    /** The default maximum allowed trace. */
    public static final int MAX_ALLOWED_TRACE_DEFAULT = 4000;

    /** The unmodified maximum allowed trace. */
    private static long MAX_ALLOWED_TRACE = MB *
        Integer.getInteger(MAX_ALLOWED_TRACE_PROPERTY_NAME, MAX_ALLOWED_TRACE_DEFAULT);

    /**
     * The maximum allowed trace, modified by running locally,
     * and modified by running in FULL mode.
     */ 
    private static long EFFECTIVE_MAX_ALLOWED_TRACE = effectiveMaxTrace(MAX_ALLOWED_TRACE);

    static {
        logValues("clinit");
    }

    private static void logValues(String methodName) {
        Log.info(c, methodName,
            "Maximum allowed trace: " + MAX_ALLOWED_TRACE / MB);        
        Log.info(c, methodName,
            "Effective maximum allowed trace: " + EFFECTIVE_MAX_ALLOWED_TRACE / MB);

        String isLocal = ( FATRunner.FAT_TEST_LOCALRUN ? "active" : "inactive" );
        Log.info(c, methodName, "Local mode reduction to 85%: " + isLocal); 

        String isFull = ( (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL) ? "active" : "inactive" );
        Log.info(c, methodName, "FULL mode tripling: " + isFull); 
    }
    
    /**
     * Sets the maximum allowed trace for a given FAT bucket. If the limit is exceeded, a test failure will be generated.
     *
     * @param maxTraceInMB The maximum allowed trace (in MB) to be produced by the current FAT
     */
    public static void setMaxAllowedTraceMB(int maxTraceInMB) {
        String methodName = "setMaxAllowedTraceMB";

        if ( maxTraceInMB > 10000 ) {
            throw new RuntimeException("You know this is max trace in MB (not KB or bytes), right?");
        }

        MAX_ALLOWED_TRACE = maxTraceInMB * MB;
        EFFECTIVE_MAX_ALLOWED_TRACE = effectiveMaxTrace(MAX_ALLOWED_TRACE);

        logValues(methodName);
    }

    //

    /** Accumulator of used trace. */
    private static long usedTrace = 0L;

    /**
     * Accumulate trace.
     *
     * @param addUsedTrace The size of a trace file, in bytes.
     */
    public static void addUsedTrace(String path, long addUsedTrace) {
        String methodName = "addUsedTrace";

        if ( addUsedTrace == 0L) {
            Log.info(c, methodName, "Used trace (MB): " + usedTrace + " (unchanged)");

        } else {
            long oldUsedTrace = usedTrace;
            usedTrace += addUsedTrace;

            Log.info(c, methodName,
                "Used trace (MB):" +
                " Old: " + (oldUsedTrace / MB) +
                " Add: " + (addUsedTrace / MB) +
                " New: " + (usedTrace / MB) + " (" + path + ")");
        }
    }

    /**
     * Test if too much trace has been used.
     * 
     * Note that the test is of the accumulated trace usage.
     * 
     * The check is made from {@link FATRunner#classBlock()} at the
     * conclusion of each test, but is made against the accumulated
     * log sizes from all test classes.
     * 
     * @throws Exception Thrown if the accumulated trace is above the
     *     maximum allowed.
     */
    static void checkUsedTrace() throws Exception {
        String methodName = "checkUsedTrace";

        Log.info(c, methodName, "So far this FAT has used " + (usedTrace / MB) + "MB of trace.");

        if ( usedTrace > EFFECTIVE_MAX_ALLOWED_TRACE )
            throw new IllegalStateException(
                "This FAT has used too much trace!" +
                "  The maximum allowed trace is " + (EFFECTIVE_MAX_ALLOWED_TRACE / MB) + " MB." +
                "  So far this FAT has used" + (usedTrace / MB) + " MB of trace.");
    }
}
