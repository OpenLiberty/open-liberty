/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.couchdb.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Contains various utility methods
 */
public class Utils {
     private static final TraceComponent tc = Tr.register(Utils.class, "couchdb");

    /**
     * Gets an NLS message.
     * 
     * @param key the message key
     * @param params the message parameters
     * @return formatted message
     */
    @Trivial
    public static final String getMessage(String key, Object... params) {
        return Tr.formatMessage(tc, key, params);
    }

    /**
     * Returns an exception message, stack, and cause formatted as a String.
     * 
     * @param x exception or error.
     * @return an exception message, stack, and cause formatted as a String.
     */
    @Trivial
    public static final String toString(Throwable x) {
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}