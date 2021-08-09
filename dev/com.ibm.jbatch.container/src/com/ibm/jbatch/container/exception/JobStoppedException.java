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
package com.ibm.jbatch.container.exception;

/**
 *
 */
public class JobStoppedException extends Exception {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    public JobStoppedException() {
        // TODO Auto-generated constructor stub
    }

    public JobStoppedException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public JobStoppedException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public JobStoppedException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
}
