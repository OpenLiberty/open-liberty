/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.taglib.GlobalTagLibraryCache;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

public class JSPExtensionProcessor extends AbstractJSPExtensionProcessor {
    public JSPExtensionProcessor(IServletContext webapp, 
                                 JspXmlExtConfig webAppConfig, 
                                 GlobalTagLibraryCache globalTagLibraryCache,
                                 JspClassloaderContext jspClassloaderContext) throws Exception {
        super(webapp, webAppConfig, globalTagLibraryCache, jspClassloaderContext);
    }

    //PK81387 - added checkWEBINF param
    protected boolean processZOSCaseCheck(String path, boolean checkWEBINF) throws IOException {
        return true;
    }

	public WebComponentMetaData getMetaData() {
		// TODO Auto-generated method stub
		return null;
	}
}
