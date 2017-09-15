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
 * Deny reply sends a 403 to deny the requested resource
 */
public class DenyReply extends WebReply {

    public DenyReply(String reason) {
        // response code 403
        super(HttpServletResponse.SC_FORBIDDEN, reason);
    }

    public void writeResponse(HttpServletResponse rsp) throws IOException {
        sendError(rsp);
    }
}
