/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.exceptions;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Exception class used for re-throwing exceptions while also logging the error in the test output.
 */
public class TestActionException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final Class<?> thisClass = TestActionException.class;

    public TestActionException(String method, String message, Throwable cause) {
        super(message + getCauseMessage(cause), cause);
        Log.error(thisClass, method, cause, "Exception occurred in " + method);
    }

    private static String getCauseMessage(Throwable cause) {
        if (cause == null) {
            return "";
        }
        Throwable originalCause = getOriginalCause(cause);
        if (originalCause == null) {
            originalCause = cause;
        }
        return " " + originalCause.toString();
    }

    private static Throwable getOriginalCause(Throwable cause) {
        if (cause == null) {
            return null;
        }
        Throwable originalCause = cause;
        Throwable currentCause = originalCause.getCause();
        while (currentCause != null) {
            originalCause = currentCause;
            currentCause = currentCause.getCause();
        }
        return originalCause;
    }

}