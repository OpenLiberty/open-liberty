/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.exception;

import java.io.IOException;

/**
 * If an outbound request uses the "Expect: 100-continue" header and the server
 * responds back with the "417 Expectation Failed" response, then this exception
 * will be thrown by the the outbound service context to inform the caller of
 * the failure.
 * 
 * @ibm-private-in-use
 */
public class ExpectationFailedException extends IOException {

    /** Serialization ID value */
    static final private long serialVersionUID = -165530030652963006L;

    /**
     * Constructor for this exception
     * 
     * @param msg
     */
    public ExpectationFailedException(String msg) {
        super(msg);
    }
}
