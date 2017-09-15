/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Base class for all replies to a web authorization request.
 */
public abstract class WebReply {
    protected int responseCode;
    public String message = null;

    protected WebReply(int code, String msg) {
        responseCode = code;
        message = msg;
    }

    protected WebReply(int code) {
        this(code, null);
    }

    public int getStatusCode() {
        return responseCode;
    }

    public abstract void writeResponse(HttpServletResponse rsp)
                    throws IOException;

    public void sendError(HttpServletResponse rsp) throws IOException {
        rsp.sendError(responseCode, message);
    }
}
