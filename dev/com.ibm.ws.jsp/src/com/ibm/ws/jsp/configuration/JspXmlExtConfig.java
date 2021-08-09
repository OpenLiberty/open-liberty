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
// defect 400645 "Batchcompiler needs to get webcon custom props"  2004/10/25 Scott Johnson
// 395182.2  70FVT: make servlet 2.3 compatible with JSP 2.1 for migration 2007/02/07 Scott Johnson
package com.ibm.ws.jsp.configuration;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.ws.jsp.JspOptions;

/**
 * @author Scott Johnson
 *
 * API for retrieving JSP configuration elements from both web.xml and the extensions document
 */
public interface JspXmlExtConfig
{
    public Map getTagLibMap();
    
    public List getJspPropertyGroups();
    
    public boolean isServlet24();
    public boolean isServlet24_or_higher();    
    public JspOptions getJspOptions();
    
    public List getJspFileExtensions();
    
    public boolean containsServletClassName(String servletClassName);
    
    //defect 400645
    public void setWebContainerProperties(Properties webConProperties);
    
    public Properties getWebContainerProperties();
    //defect 400645
    
    //only used during runtime when JCDI will use this to determine whether to wrap the ExpressionFactory
    public void setJCDIEnabledForRuntimeCheck(boolean b);
    public boolean isJCDIEnabledForRuntimeCheck();
}
