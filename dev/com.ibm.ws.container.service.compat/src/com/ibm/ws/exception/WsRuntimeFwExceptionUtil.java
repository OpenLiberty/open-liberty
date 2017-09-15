/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.exception;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Runtime framework internal use only. This class is intended to avoid
 * redundantly printing exception stack traces as <tt>WsRuntimeFwException</tt>s
 * pass through the runtime framework. Ideally,
 * the runtime framework would not print these exceptions at all, instead
 * relying on the components that throw them to print a message if necessary.
 */
public class WsRuntimeFwExceptionUtil {
    private static final TraceComponent tc = Tr.register(WsRuntimeFwException.class, "Runtime");

    private WsRuntimeFwExceptionUtil() {}

    /**
     * Sets the reported status of the exception to <tt>true</tt> and returns <tt>true</tt> if this exception should be reported.
     * 
     * @return <tt>true</tt> if this exception should be printed in a message
     */
    public static boolean report(WsRuntimeFwException ex) {
        if (ex.reported) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "previously reported exception", new Exception(ex));
            }
            return false;
        }

        ex.reported = true;
        return true;
    }

    /**
     * Sets the reported status of the exception to the specified value.
     * 
     * @param reported
     *            the reported status
     * @see #report()
     */
    public static void setReported(WsRuntimeFwException ex, boolean reported) {
        ex.reported = reported;
    }
}
