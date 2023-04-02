/*******************************************************************************
 * Copyright (c) 2013,2022 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Utility class
 */
@Trivial
public class Utils {
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
