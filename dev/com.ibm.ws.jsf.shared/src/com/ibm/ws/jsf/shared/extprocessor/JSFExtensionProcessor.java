/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.shared.extprocessor;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

//import com.ibm.wsspi.webcontainer.extension.WebExtensionProcessor;
import com.ibm.ws.webcontainer.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class JSFExtensionProcessor extends WebExtensionProcessor {

    public JSFExtensionProcessor(IServletContext webapp) {
        super(webapp);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.RequestProcessor#handleRequest(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        throw new IllegalStateException("JSFExtensionProcessor.handleRequest(ServletRequest, ServletResponse) is not implemented by this extension processor");
    }

}