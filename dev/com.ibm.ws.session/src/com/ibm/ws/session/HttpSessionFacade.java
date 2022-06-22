/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/*
 * Updated for Servlet 6.0: Specific API for session up to servlet 5.0
 * Common methods are moved to AbstractHttpSessionFacade
 */

public class HttpSessionFacade extends AbstractHttpSessionFacade { // cmd 196151

    private static final long serialVersionUID = 3108339284895967670L;

    public HttpSessionFacade(AbstractSessionData data) {
        super(data);
    }

    /**
     * @see HttpSession#getSessionContext()
     * @deprecated
     */
    public HttpSessionContext getSessionContext() {
        return getSessionData().getSessionContext();
    }

    /**
     * @see HttpSession#getValue(String)
     * @deprecated
     */
    public Object getValue(String arg0) {
        return getSessionData().getValue(arg0);
    }


    /**
     * @see HttpSession#getValueNames()
     * @deprecated
     */
    public String[] getValueNames() {
        return getSessionData().getValueNames();
    }

    /**
     * @see HttpSession#putValue(String, Object)
     * @deprecated
     */
    public void putValue(String arg0, Object arg1) {
        getSessionData().putValue(arg0, arg1);
    }


    /**
     * @see HttpSession#removeValue(String)
     * @deprecated
     */
    public void removeValue(String arg0) {
        getSessionData().removeValue(arg0);
    }

}
