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
package com.ibm.ws.webcontainer.exception;

import java.text.MessageFormat;

import com.ibm.ejs.ras.TraceNLS;

//Liberty - Change import
//import com.ibm.ejs.sm.client.ui.NLS;


public class WebGroupVHostNotFoundException extends WebContainerException {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 4120848863067584569L;
    // Liberty - Changed to TraceNLS
    //private static NLS nls = new NLS("com.ibm.ws.webcontainer.resources.Messages");
    private static TraceNLS nls = TraceNLS.getTraceNLS(WebGroupVHostNotFoundException.class, "com.ibm.ws.webcontainer.resources.Messages");

    public WebGroupVHostNotFoundException(String s)
    {     
        super(MessageFormat.format(nls.getString("Web.Group.VHost.Not.Found", "A WebGroup/Virtual Host to handle {0} has not been defined."), new Object[]{s}));
    }
}
