/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

/**
 * Exception thrown if the byte buffer service's configuration is
 * found to be incorrect.
 */
public class WsBBConfigException extends Exception {
    // required SUID since Exception extends Throwable, which is serializable
    private static final long serialVersionUID = 5562203322439138676L;

    /**
     * Constructor for WsBBConfigException.
     * 
     * @param message
     */
    public WsBBConfigException(String message) {
        super(message);
    }

    /**
     * Constructor for WsBBConfigException.
     * 
     * @param message
     * @param t throwable to wrap around
     */
    public WsBBConfigException(String message, Throwable t) {
        super(message, t);
    }

}
