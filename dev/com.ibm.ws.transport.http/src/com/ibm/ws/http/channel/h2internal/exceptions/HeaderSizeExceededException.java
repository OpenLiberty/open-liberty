/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.exceptions;

/**
 * Exception to track compression exceptions that are related to header limits
 */
public class HeaderSizeExceededException extends CompressionException {
    private static final long serialVersionUID = 1L;

    public HeaderSizeExceededException(String s) {
        super(s);
    }

}
