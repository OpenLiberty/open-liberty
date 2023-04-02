/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Web reply to return the response object as is, regardless of
 * HTTP status code (originally created for JASPI 1.1 forward/include)
 */
public class ReturnReply extends WebReply {

    public ReturnReply(int code, String msg) {
        super(code, msg);
    }

    /**
     * Response may be committed so no writing
     */
    @Override
    public void writeResponse(HttpServletResponse rsp) throws IOException {}
}
