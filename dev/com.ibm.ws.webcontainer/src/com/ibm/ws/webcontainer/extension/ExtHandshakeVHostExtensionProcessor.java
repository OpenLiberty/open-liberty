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
package com.ibm.ws.webcontainer.extension;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

public class ExtHandshakeVHostExtensionProcessor implements ExtensionProcessor {

    public ExtHandshakeVHostExtensionProcessor() {
        
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionProcessor#getPatternList()
     */
    @SuppressWarnings("unchecked")
    public List getPatternList() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.RequestProcessor#handleRequest(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
 
    }

	public String getName() {
		return "ExtHandshakeVHostExtensionProcessor";
	}

	public boolean isInternal() {
		return false;
	}

	public IServletWrapper getServletWrapper(ServletRequest req,
			ServletResponse resp) {
		// TODO Auto-generated method stub
		return null;
	}

	public WebComponentMetaData getMetaData() {
		// TODO Auto-generated method stub
		return null;
    }

}
