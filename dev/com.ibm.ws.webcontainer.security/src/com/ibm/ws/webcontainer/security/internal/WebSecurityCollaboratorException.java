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

public class WebSecurityCollaboratorException extends Exception {
    private static final long serialVersionUID = 4879760322028996722L;

    private transient final WebReply wReply;

    private transient Object wSecurityContext;

    public WebSecurityCollaboratorException(WebReply reply) {
        this(null, reply, null);
    }

    public WebSecurityCollaboratorException(String msg, WebReply reply) {
        this(msg, reply, null);
    }

    public WebSecurityCollaboratorException(String msg, WebReply reply, Object securityContext) {
        super(msg);
        wReply = reply;
        wSecurityContext = securityContext;
    }

    public WebReply getWebReply() {
        return wReply;
    }

    public Object getWebSecurityContext() {
        return wSecurityContext;
    }
}
