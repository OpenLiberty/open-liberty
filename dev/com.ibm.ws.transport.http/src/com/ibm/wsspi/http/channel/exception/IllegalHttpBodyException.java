/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package com.ibm.wsspi.http.channel.exception;

import java.io.IOException;

/**
 * Thrown while reading the body of the HTTP object which is
 * invalid, such as a non-length delimited request body
 * 
 * @ibm-private-in-use
 */
public class IllegalHttpBodyException extends IOException {

    /** Serialization ID value */
    static final private long serialVersionUID = -7287468367768043941L;

    /**
     * Constructor for this exception
     * 
     * @param msg
     */
    public IllegalHttpBodyException(String msg) {
        super(msg);
    }
}
