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
package org.apache.jasper.runtime;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

public class JcdiWrappedJspFactoryImpl extends JspFactoryImpl {
    
    JspFactory instance = null;
    
    public JcdiWrappedJspFactoryImpl(JspFactory impl) {
        instance = impl;
    }

    public JspApplicationContext getJspApplicationContext(ServletContext context) {
        return JcdiWrappedJspApplicationContextImpl.getInstance(context);  
    }
    
    public PageContext getPageContext(
            Servlet servlet,
            ServletRequest request,
            ServletResponse response,
            String errorPageURL,
            boolean needsSession,
            int bufferSize,
            boolean autoflush) {
        return instance.getPageContext(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
    }

    @Override
    public JspEngineInfo getEngineInfo() {
        return instance.getEngineInfo();
    }

    @Override
    public void releasePageContext(PageContext pc) {
        instance.releasePageContext(pc);
    }
    
}
