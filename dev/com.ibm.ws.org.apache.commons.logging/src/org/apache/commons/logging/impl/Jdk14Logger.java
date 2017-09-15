/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//logging/src/java/org/apache/commons/logging/impl/Jdk14Logger.java,v 1.6 2003/03/31 00:27:08 craigmcc Exp $
 * $Revision: 1.6 $
 * $Date: 2003-03-30 16:27:08 -0800 (Sun, 30 Mar 2003) $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */


package org.apache.commons.logging.impl;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;


/**
 * <p>Implementation of the <code>org.apache.commons.logging.Log</code>
 * interfaces that wraps the standard JDK logging mechanisms that were
 * introduced in the Merlin release (JDK 1.4).</p>
 *
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version $Revision: 1.6 $ $Date: 2003-03-30 16:27:08 -0800 (Sun, 30 Mar 2003) $
 */

public final class Jdk14Logger implements Log {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a named instance of this Logger.
     *
     * @param name Name of the logger to be constructed
     */
    public Jdk14Logger(String name) {

        logger = Logger.getLogger(name);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The underlying Logger implementation we are using.
     */
    protected Logger logger = null;


    // --------------------------------------------------------- Public Methods

    private void log( Level level, String msg, Throwable ex ) {
        if (logger.isLoggable(level)) {
            // Hack (?) to get the stack trace.
            Throwable dummyException=new Throwable();
            StackTraceElement locations[]=dummyException.getStackTrace();
            // Caller will be the third element
            String cname="unknown";
            String method="unknown";
            if( locations!=null && locations.length >2 ) {
                StackTraceElement caller=locations[2];
                cname=caller.getClassName();
                method=caller.getMethodName();
            }
            if( ex==null ) {
                logger.logp( level, cname, method, msg );
            } else {
                logger.logp( level, cname, method, msg, ex );
            }
        }
    }

    /**
     * Log a message with debug log level.
     */
    public void debug(Object message) {
        log(Level.FINE, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with debug log level.
     */
    public void debug(Object message, Throwable exception) {
        log(Level.FINE, String.valueOf(message), exception);
    }


    /**
     * Log a message with error log level.
     */
    public void error(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with error log level.
     */
    public void error(Object message, Throwable exception) {
        log(Level.SEVERE, String.valueOf(message), exception);
    }


    /**
     * Log a message with fatal log level.
     */
    public void fatal(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with fatal log level.
     */
    public void fatal(Object message, Throwable exception) {
        log(Level.SEVERE, String.valueOf(message), exception);
    }


    /**
     * Return the native Logger instance we are using.
     */
    public Logger getLogger() {
        return (this.logger);
    }


    /**
     * Log a message with info log level.
     */
    public void info(Object message) {
        log(Level.INFO, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with info log level.
     */
    public void info(Object message, Throwable exception) {
        log(Level.INFO, String.valueOf(message), exception);
    }


    /**
     * Is debug logging currently enabled?
     */
    public boolean isDebugEnabled() {
        return (logger.isLoggable(Level.FINE));
    }


    /**
     * Is error logging currently enabled?
     */
    public boolean isErrorEnabled() {
        return (logger.isLoggable(Level.SEVERE));
    }


    /**
     * Is fatal logging currently enabled?
     */
    public boolean isFatalEnabled() {
        return (logger.isLoggable(Level.SEVERE));
    }


    /**
     * Is info logging currently enabled?
     */
    public boolean isInfoEnabled() {
        return (logger.isLoggable(Level.INFO));
    }


    /**
     * Is tace logging currently enabled?
     */
    public boolean isTraceEnabled() {
        return (logger.isLoggable(Level.FINEST));
    }


    /**
     * Is warning logging currently enabled?
     */
    public boolean isWarnEnabled() {
        return (logger.isLoggable(Level.WARNING));
    }


    /**
     * Log a message with trace log level.
     */
    public void trace(Object message) {
        log(Level.FINEST, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with trace log level.
     */
    public void trace(Object message, Throwable exception) {
        log(Level.FINEST, String.valueOf(message), exception);
    }


    /**
     * Log a message with warn log level.
     */
    public void warn(Object message) {
        log(Level.WARNING, String.valueOf(message), null);
    }


    /**
     * Log a message and exception with warn log level.
     */
    public void warn(Object message, Throwable exception) {
        log(Level.WARNING, String.valueOf(message), exception);
    }


}
