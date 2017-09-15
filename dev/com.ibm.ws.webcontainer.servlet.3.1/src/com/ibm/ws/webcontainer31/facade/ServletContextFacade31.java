/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.facade;

import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * @author asisin
 *
 * Facade wrapping the WebApp when returning a context to the user. This will 
 * prevent users from exploiting public methods in WebApp which were intended
 * for internal use only.
 */
public class ServletContextFacade31 extends ServletContextFacade {

    //private static TraceNLS nls = TraceNLS.getTraceNLS(ServletContextFacade31.class, "com.ibm.ws.webcontainer.resources.Messages");

    public ServletContextFacade31(IServletContext context) {
        super(context);
    }



    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getVirtualServerName()
     */
    //@Override
    public String getVirtualServerName() {
        return context.getVirtualServerName();
    }

}
