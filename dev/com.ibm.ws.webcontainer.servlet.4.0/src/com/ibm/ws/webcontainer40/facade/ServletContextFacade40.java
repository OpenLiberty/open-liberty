/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.facade;

import javax.servlet.ServletRegistration;

import com.ibm.ws.webcontainer31.facade.ServletContextFacade31;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Facade wrapping the WebApp when returning a context to the user. This will
 * prevent users from exploiting public methods in WebApp which were intended
 * for internal use only.
 */
public class ServletContextFacade40 extends ServletContextFacade31 {

    public ServletContextFacade40(IServletContext context) {
        super(context);
    }

    /**
     * @see javax.servlet.ServletContext#getSessionTimeout()
     */
    @Override
    public int getSessionTimeout() {
        return context.getSessionTimeout();
    }

    /**
     * @see javax.servlet.ServletContext#setSessionTimeout(int sessionTimeout)
     */
    @Override
    public void setSessionTimeout(int sessionTimeout) {
        context.setSessionTimeout(sessionTimeout);
    }

    /**
     * @see javax.servlet.ServletContext#getRequestCharacterEncoding()
     */
    @Override
    public String getRequestCharacterEncoding() {
        return context.getRequestCharacterEncoding();
    }

    /**
     * @see javax.servlet.ServletContext#setRequestCharacterEncoding(String encoding)
     */
    @Override
    public void setRequestCharacterEncoding(String encoding) {
        context.setRequestCharacterEncoding(encoding);
    }

    /**
     * @see javax.servlet.ServletContext#getResponseCharacterEncoding()
     */
    @Override
    public String getResponseCharacterEncoding() {
        return context.getResponseCharacterEncoding();
    }

    /**
     * @see javax.servlet.ServletContext#setResponseCharacterEncoding(String encoding)
     */
    @Override
    public void setResponseCharacterEncoding(String encoding) {
        context.setResponseCharacterEncoding(encoding);
    }

    /**
     * @see javax.servlet.ServletContext#addJspFile(String servletName, String jspFile)
     */
    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        return context.addJspFile(servletName, jspFile);
    }

}
