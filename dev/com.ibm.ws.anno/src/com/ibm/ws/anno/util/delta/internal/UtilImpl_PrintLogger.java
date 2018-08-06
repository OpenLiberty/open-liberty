/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.util.delta.internal;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.anno.util.Util_PrintLogger;

public class UtilImpl_PrintLogger implements Util_PrintLogger {

    public UtilImpl_PrintLogger(Logger logger) {
        this.logger = logger;
        this.writer = null;
    }

    public UtilImpl_PrintLogger(PrintWriter writer) {
        this.logger = null;
        this.writer = writer;
    }

    private final Logger logger;

    @Override
    public Logger getLogger() {
        return logger;
    }

    private final PrintWriter writer;

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public boolean isLoggable(Level level) {
        return ( (logger == null) || logger.isLoggable(level) );
    }

    @Override
    public void logp(Level level, String className, String methodName, String message) {
        if ( logger != null ) {
            if ( logger.isLoggable(level) ) {
                logger.logp(level, className, methodName, message);
            } else {
                // Not enabled; ignore.
            }

        } else { // Always enabled for a print writer.
            writer.println(message);
        }
    }

    @Override
    public void logp(Level level, String className, String methodName, String message, Object...parms) {
        if ( logger != null ) {
            if ( logger.isLoggable(level) ) {
                logger.logp( level, className, methodName, MessageFormat.format(message,  parms) );
            } else {
                // Not enabled; ignore.
            }

        } else { // Always enabled for a print writer.
            writer.println( MessageFormat.format(message,  parms) );
        }
    }
}
