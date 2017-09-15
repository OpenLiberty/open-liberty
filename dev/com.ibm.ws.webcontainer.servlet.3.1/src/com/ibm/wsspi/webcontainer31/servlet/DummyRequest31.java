/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer31.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUpgradeHandler;

import com.ibm.wsspi.webcontainer.servlet.DummyRequest;

public class DummyRequest31 extends DummyRequest implements HttpServletRequest {


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getContentLengthLong()
     */
    @Override
    public long getContentLengthLong() {
        // Added for Servlet 3.1
        return 0L;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#changeSessionId()
     */
    @Override
    public String changeSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#upgrade(java.lang.Class)
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

}
