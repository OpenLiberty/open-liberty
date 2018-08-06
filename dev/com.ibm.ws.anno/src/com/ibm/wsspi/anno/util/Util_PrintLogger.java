/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.anno.util;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface Util_PrintLogger {

    Logger getLogger();
    PrintWriter getWriter();

    boolean isLoggable(Level level);

    void logp(Level level, String className, String methodName, String message);
    void logp(Level level, String className, String methodName, String message, Object... parms);
}
