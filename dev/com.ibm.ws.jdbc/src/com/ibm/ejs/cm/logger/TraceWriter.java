/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.cm.logger;

import java.io.*;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class TraceWriter extends StringWriter {

    /**
     * Class constructor
     * 
     * @param dest TraceComponent: which is the trace component the destination intialized
     */
    public TraceWriter(TraceComponent dest) { 
        destination = dest;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // StringWriter overrides
    //

    public void flush() {
        synchronized (lock) {
            super.flush();
            formatTrace();
        }
    }

    public void write(char[] cbuf, int off, int len) {
        synchronized (lock) {
            super.write(cbuf, off, len);
            formatTrace();
        }
    }

    public void write(int b) {
        synchronized (lock) {
            super.write(b);
            if (b == '\n') {
                formatTrace();
            }
        }
    }

    public void write(String str) {
        synchronized (lock) {
            super.write(str);
            formatTrace();
        }
    }

    public void write(String str, int off, int len) {
        synchronized (lock) {
            super.write(str, off, len);
            formatTrace();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Implementation
    //

    public boolean isTraceEnabled() {
        return (destination.isDebugEnabled());
    }

    private void formatTrace() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "formatTrace");

        final String str = toString();
        int start = 0;
        for (int end = str.indexOf('\n'); end >= 0; start = end + 1, end = str.indexOf('\n', start)) {
            Tr.debug(destination, str.substring(start, end));
        }

        getBuffer().delete(0, start);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "formatTrace");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Data
    //

    /** Trace component where output is directed */
    private final TraceComponent destination;

    /** Trace component */
    private static final TraceComponent tc = Tr.register(TraceWriter.class);
}
