/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

/**
 * Exception for returning multiple failures from a test
 */
public class MultipleFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MultipleFailureException(String message) {
        super(message, // Message
              null, // No cause
              true, // Enable suppression so that all exceptions appear in the stack trace
              true); // Enable stack traces
    }

    public void addFailure(Throwable t) {
        addSuppressed(t);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        for (Throwable t : getSuppressed()) {
            sb.append("\n");
            sb.append("    ");
            sb.append(t);
        }
        return sb.toString();
    }

}
