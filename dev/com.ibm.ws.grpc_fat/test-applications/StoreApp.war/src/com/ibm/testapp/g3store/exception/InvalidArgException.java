/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.exception;

/**
 * @author anupag
 *
 */
public class InvalidArgException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for this exception
     * 
     * @param msg
     */
    public InvalidArgException(String msg) {
        super(msg);
    }

    public InvalidArgException(Throwable t) {
        super(t.getMessage(), t);
    }

    public InvalidArgException(String msg, Throwable t) {
        super(msg, t);
    }

    public InvalidArgException() {
        super();
    }

}
