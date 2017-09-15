/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.exceptions;

import com.ibm.ws.http.channel.h2internal.Constants;

/**
 *
 */
public class HTTP11RequiredException extends Http2Exception {

    private static final long serialVersionUID = 568265670726965097L;

    int errorCode = Constants.HTTP_1_1_REQUIRED;
    String errorString = "HTTP_1_1_REQUIRED";

    public HTTP11RequiredException(String s) {
        super(s);
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorString() {
        return errorString;
    }
}
