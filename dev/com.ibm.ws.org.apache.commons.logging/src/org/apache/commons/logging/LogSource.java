/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//logging/src/java/org/apache/commons/logging/LogSource.java,v 1.17 2003/03/30 23:42:36 craigmcc Exp $
 * $Revision: 1.17 $
 * $Date: 2003-03-30 15:42:36 -0800 (Sun, 30 Mar 2003) $
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

package org.apache.commons.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.impl.Jdk14Logger;

/**
 * <p>Factory for creating {@link Log} instances. Applications should call
 * the <code>makeNewLogInstance()</code> method to instantiate new instances
 * of the configured {@link Log} implementation class.</p>
 * 
 * <p>By default, calling <code>getInstance()</code> will use the following
 * algorithm:</p>
 * <ul>
 * <li>If Log4J is available, return an instance of
 * <code>org.apache.commons.logging.impl.Log4JLogger</code>.</li>
 * <li>If JDK 1.4 or later is available, return an instance of
 * <code>org.apache.commons.logging.impl.Jdk14Logger</code>.</li>
 * <li>Otherwise, return an instance of
 * <code>org.apache.commons.logging.impl.NoOpLog</code>.</li>
 * </ul>
 * 
 * <p>You can change the default behavior in one of two ways:</p>
 * <ul>
 * <li>On the startup command line, set the system property
 * <code>org.apache.commons.logging.log</code> to the name of the
 * <code>org.apache.commons.logging.Log</code> implementation class
 * you want to use.</li>
 * <li>At runtime, call <code>LogSource.setLogImplementation()</code>.</li>
 * </ul>
 * 
 * @deprecated Use {@link LogFactory} instead - The default factory
 *             implementation performs exactly the same algorithm as this class did
 * 
 * @author Rod Waldhoff
 * @version $Original-Id: LogSource.java 138961 2003-03-30 23:42:36Z craigmcc $
 */
@Deprecated
public class LogSource {

    // ------------------------------------------------------- Class Attributes

    static private final ConcurrentMap<String, Log> logs = new ConcurrentHashMap<String, Log>();

    // ----------------------------------------------------- Class Initializers

    // ------------------------------------------------------------ Constructor

    /** Don't allow others to create instances */
    private LogSource() {}

    // ---------------------------------------------------------- Class Methods

    /**
     * Set the log implementation/log implementation factory
     * by the name of the class. The given class
     * must implement {@link Log}, and provide a constructor that
     * takes a single {@link String} argument (containing the name
     * of the log).
     */
    static public void setLogImplementation(String classname) throws
                    LinkageError, ExceptionInInitializerError,
                    NoSuchMethodException, SecurityException,
                    ClassNotFoundException {}

    /**
     * Set the log implementation/log implementation factory
     * by class. The given class must implement {@link Log},
     * and provide a constructor that takes a single {@link String} argument (containing the name of the log).
     */
    static public void setLogImplementation(Class logclass) throws
                    LinkageError, ExceptionInInitializerError,
                    NoSuchMethodException, SecurityException {}

    /** Get a <code>Log</code> instance by class name */
    static public Log getInstance(String name) {
        Log log = (logs.get(name));
        if (null == log) {
            log = makeNewLogInstance(name);
            Log log2 = logs.putIfAbsent(name, log);
            if (log2 != null) {
                return log2;
            }
        }
        return log;
    }

    /** Get a <code>Log</code> instance by class */
    static public Log getInstance(Class clazz) {
        return getInstance(clazz.getName());
    }

    /**
     * Create a new {@link Log} implementation, based
     * on the given <i>name</i>.
     * <p>
     * The specific {@link Log} implementation returned
     * is determined by the value of the
     * <tt>org.apache.commons.logging.log</tt> property.
     * The value of <tt>org.apache.commons.logging.log</tt> may be set to
     * the fully specified name of a class that implements
     * the {@link Log} interface. This class must also
     * have a public constructor that takes a single {@link String} argument (containing the <i>name</i>
     * of the {@link Log} to be constructed.
     * <p>
     * When <tt>org.apache.commons.logging.log</tt> is not set,
     * or when no corresponding class can be found,
     * this method will return a Log4JLogger
     * if the log4j Logger class is
     * available in the {@link LogSource}'s classpath, or a
     * Jdk14Logger if we are on a JDK 1.4 or later system, or
     * NoOpLog if neither of the above conditions is true.
     * 
     * @param name the log name (or category)
     */
    static public Log makeNewLogInstance(String name) {

        Log log = new Jdk14Logger(name);
        return log;

    }

    /**
     * Returns a {@link String} array containing the names of
     * all logs known to me.
     */
    static public String[] getLogNames() {
        return (logs.keySet().toArray(new String[logs.size()]));
    }

}
