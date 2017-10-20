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
package test.common.zos;

/**
 *
 */
public class NativeLibraryTraceHelper {

    /**
     * Initialize the unittest environment.
     * 
     */
    public static int init() throws Exception {

        //Check the environment for the tracing properties, if the traceLevel is not specified 
        //or is equal to zero unittest trace is NOT enabled.

        int rc = 0;
        int traceLevel = 0;

        String strTraceLevel = System.getenv("ZOS_NATIVE_UNIT_TEST_ENV_TRACELEVEL");
        if (strTraceLevel != null) {
            try {
                traceLevel = Integer.parseInt(strTraceLevel);
            } catch (NumberFormatException nfe) {
            }
        }

        //
        //The log file is required if unittest trace level is > 0.   
        //
        String strLogFilePathName = System.getenv("ZOS_NATIVE_UNIT_TEST_ENV_TRACE_FILENAME");
        if (strLogFilePathName == null && traceLevel > 0) {
            throw new IllegalStateException("NativeLibraryUtils init() failed. ZOS_NATIVE_UNIT_TEST_ENV_TRACE_FILENAME is null");
        }

        // If trace is not enabled then set to empty string
        if (strLogFilePathName == null)
            strLogFilePathName = "";

        NativeLibraryUtils.registerNatives(NativeLibraryTraceHelper.class);
        //push the trace properties down into native 
        rc = NativeLibraryTraceHelper.ntv_initTraceForUnitTest(traceLevel, strLogFilePathName);
        return rc;
    }

    /**
     * Reset the unittest environment to it's initial state.
     * 
     */
    public static int reset() throws Exception {

        //Reset any trace state back to its original pristine state.  
        int rc = 0;
        rc = NativeLibraryTraceHelper.ntv_resetTraceForUnitTest();
        return rc;

    }

    //
    // Initial the unit test trace environment with the trace level and fully qualified path + filename 
    // of the location of the trace log file.
    //
    private final static native int ntv_initTraceForUnitTest(int traceLevel, String logFilePathName);

    //
    // Reset the unit test trace environment to its inital state.     
    //
    private final static native int ntv_resetTraceForUnitTest();

}
