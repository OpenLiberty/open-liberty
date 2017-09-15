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

package com.ibm.ws.session.http;

import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class HttpSessionContextImpl implements HttpSessionContext {

    // ----------------------------------------
    // Class Constructor
    // ----------------------------------------
    public HttpSessionContextImpl() {
        super();
    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Method getSession
     * 
     * @deprecated @see
     *             javax.servlet.http.HttpSessionContext#getSession(java.lang.
     *             String)
     */
    public HttpSession getSession(String arg0) {
        return null;
    }

    /**
     * Method getIds
     * 
     * @deprecated @see javax.servlet.http.HttpSessionContext#getIds()
     */
    public Enumeration getIds() {
        Vector v = new Vector();
        return v.elements();
    }

}