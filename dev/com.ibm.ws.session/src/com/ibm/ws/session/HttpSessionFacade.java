/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
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
package com.ibm.ws.session;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/*
 * Updated for Servlet 6.0: Specific API for session up to servlet 5.0
 * Common methods are moved to AbstractHttpSessionFacade
 */

@SuppressWarnings("deprecation")
public class HttpSessionFacade extends AbstractHttpSessionFacade { // cmd 196151

    private static final long serialVersionUID = 3108339284895967670L;

    public HttpSessionFacade(SessionData data) {
        super(data);
    }

    /**
     * @see HttpSession#getSessionContext()
     * @deprecated
     */
    @Override
    public final HttpSessionContext getSessionContext() {
        return _session.getSessionContext();
    }

    /**
     * @see HttpSession#getValue(String)
     * @deprecated
     */
    @Override
    public final Object getValue(String arg0) {
        return _session.getValue(arg0);
    }

    /**
     * @see HttpSession#getValueNames()
     * @deprecated
     */
    @Override
    public final String[] getValueNames() {
        return _session.getValueNames();
    }

    /**
     * @see HttpSession#putValue(String, Object)
     * @deprecated
     */
    @Override
    public final void putValue(String arg0, Object arg1) {
        _session.putValue(arg0, arg1);
    }


    /**
     * @see HttpSession#removeValue(String)
     * @deprecated
     */
    @Override
    public final void removeValue(String arg0) {
        _session.removeValue(arg0);
    }
}
