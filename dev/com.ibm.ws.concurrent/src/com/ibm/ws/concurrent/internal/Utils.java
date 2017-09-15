/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Utility class
 */
@Trivial
public class Utils {
    /**
     * Returns a textual representation of a date that includes the number of milliseconds
     * 
     * @param date the date
     * @return a textual representation of a date that includes the number of milliseconds
     */
    public static final String toString(Date date) {
        return date == null ? null : (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS z").format(date) + ' ' + date.getTime());
    }

    /**
     * Returns text that uniquely identifies a thread context instance. This is only used for tracing.
     * 
     * @param c captured thread context
     * @return text formatted to include the class name and hashcode.
     */
    @Trivial
    static String toString(ThreadContext c) {
        if (c == null)
            return null;
        String s = c.toString();
        if (s.indexOf('@') < 0)
            s = c.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(c)) + ':' + s;
        return s;
    }

    private static final String EOLN = String.format("%n");

    /**
     * Formats an exception's stack trace as a String.
     * 
     * @param th a throwable object (Exception or Error)
     * 
     * @return String containing the exception's stack trace.
     */
    public static final String toString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (int depth = 0; depth < 10 && th != null; depth++) {
            th.printStackTrace(pw);
            Throwable cause = th.getCause();
            if (cause != null && cause != th) {
                pw.append("-------- chained exception -------").append(EOLN);
            }
            th = cause;
        }
        return sw.toString();
    }
}
