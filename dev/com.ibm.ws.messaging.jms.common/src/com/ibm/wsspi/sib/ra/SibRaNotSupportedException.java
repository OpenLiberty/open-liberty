/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

package com.ibm.wsspi.sib.ra;

/**
 * Exception thrown when a method is invoked that is not supported by the core
 * SPI resource adapter.
 */
public class SibRaNotSupportedException extends RuntimeException {
  
    // Added at version 1.8
    private static final long serialVersionUID = -3063679180201169050L;

    /**
     * Constructor.
     */
    public SibRaNotSupportedException() {

        super();

    }

    /**
     * Constructor.
     * 
     * @param msg
     *            the exception message
     */
    public SibRaNotSupportedException(String msg) {

        super(msg);

    }

    /**
     * Constructor.
     * 
     * @param throwable
     *            the cause
     */
    public SibRaNotSupportedException(Throwable throwable) {

        super(throwable);

    }

    /**
     * Constructor.
     * 
     * @param msg
     *            the exception message
     * @param throwable
     *            the cause
     */
    public SibRaNotSupportedException(String msg, Throwable throwable) {

        super(msg, throwable);

    }

}
