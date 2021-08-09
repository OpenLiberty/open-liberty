/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.converged;

import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.session.HttpSessionFacade;
import com.ibm.ws.session.SessionData;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionImpl;

public class WsHttpSessionFacade extends HttpSessionFacade implements ConvergedHttpSession {

    private static final long serialVersionUID = 3108339284895967670L;

    public WsHttpSessionFacade(SessionData data) {
        super(data);
    }

    public String encodeURL(String url) {
        return ((ConvergedHttpSessionImpl)_session).encodeURL(url);
    }

    public String encodeURL(String relativePath, String scheme) {
        return ((ConvergedHttpSessionImpl)_session).encodeURL(relativePath, scheme);
    }

    public SipApplicationSession getApplicationSession() {
        return ((ConvergedHttpSessionImpl)_session).getApplicationSession();
    }

}
