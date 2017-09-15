/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package com.ibm.ws.logging.internal.impl;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 */
public class SharedLogHandler extends Handler {
    public ArrayList<LogRecord> traceRecords = new ArrayList<LogRecord>();

    public void addHandlerToLogger(Logger lg) {
        boolean handlerFound = false;

        // Check handlers, remove other SharedLogHandlers
        Handler h[] = lg.getHandlers();
        for (int i = 0; i < h.length; i++) {
            if (h[i] == this)
                handlerFound = true;
            else if (h[i] instanceof SharedLogHandler)
                lg.removeHandler(h[i]);
        }

        // Now add our handler if we didn't find it
        if (!handlerFound)
            lg.addHandler(this);
    }

    public void clearResult() {
        traceRecords.clear();
    }

    @Override
    public void publish(LogRecord record) {
        traceRecords.add(record);
    }

    @Override
    public void close() throws SecurityException {}

    @Override
    public void flush() {}
}
