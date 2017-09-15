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
 * On a successful authentication and authorization, the WebCollaborator will
 * result in permitting the request to go through. Setting up credential (used
 * in AppServers), cookies (for single sign-on), auth type and remote user (for
 * Servlet API requirements) are all encapsulated within the PermitReply.
 */
public class PermitReply extends WebReply {

    public PermitReply() {
        super(HttpServletResponse.SC_OK, "OK");
    }

    @Override
    public void writeResponse(HttpServletResponse rsp) throws IOException {}
}
