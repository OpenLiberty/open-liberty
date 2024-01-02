/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.jasper.runtime;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspEngineInfo;
import jakarta.servlet.jsp.JspFactory;
import jakarta.servlet.jsp.PageContext;

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
