/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.util.Vector;

/**
 * holds the last 50 errors encountered by a log handler
 */
public class LoggingHandlerDiagnosticsVector {

    protected static final int VECTOR_LIMIT = 50;

    /**
     * vector of errors
     */
    protected Vector<LoggingHandlerDianostics> savedDiagnostics = null;

    /**
     * constructor
     */
    public LoggingHandlerDiagnosticsVector() {
        savedDiagnostics = new Vector<LoggingHandlerDianostics>();
    }

    /**
     * Inserts the specified element at the beginning of the Vector.
     * If the number of components in this vector is greater than or equal to 50, the last element is removed
     * before the specified one is added at the beginning.
     *
     */
    public void insertElementAtBegining(String englishMsg, int writeReturnCode) {
        // only save last 50. do not want a storage leak storage if we keep failing
        if (savedDiagnostics.size() >= VECTOR_LIMIT) {
            savedDiagnostics.removeElementAt(savedDiagnostics.size() - 1);
        }
        savedDiagnostics.add(0, new LoggingHandlerDianostics(englishMsg, writeReturnCode));
    }

}

/**
 * hold errors encountered by a log handler
 */
class LoggingHandlerDianostics {

    protected String msg;
    protected int rc;

    /**
     * @param message
     * @param returnCode
     * @param throwable
     */
    public LoggingHandlerDianostics(String message, int returnCode) {
        msg = message;
        rc = returnCode;
    }

}
