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
package com.ibm.wsspi.jsp.context.translation;

import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.*;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;

/**
 * The JSP Container provides a object implemeting this interface and sets it within the 
 * JspTranslationContext object. It can be used to obtain access to the default factories
 * provided by the JSP Container along with other utility methods.
 */
public interface JspTranslationEnvironment {
    /**
     * Returns the path to the output directory used to store generated jsp servlets.
     * 
     * @return String - output directory
     */
    String getOutputDir();
    
    /**
     * Returns the context root of the web application that jsps are being served from.
     * 
     * @return String - context root of the web application 
     */
    String getContextRoot();
    
    /**
     * Returns a mangled version of a jsp filename that can be used for creating valid
     * class names
     * 
     * @param jspFileName - String 
     * @return
     */
    String mangleClassName(String jspFileName);
    
    /**
     * Returns the default JspInputSourceFactory object
     * 
     * @return JspInputSourceFactory
     */
    JspInputSourceFactory getDefaultJspInputSourceFactory();
    
    /**
     * Returns the defailt JspResourceFactrory object
     *  
     * @return JspResourcesFactory
     */
    JspResourcesFactory getDefaultJspResourcesFactory();
    
    /**
     * Returns the default JspClassloaderContext object
     * 
     * @return JspClassloaderContext
     */
    JspClassloaderContext getDefaultJspClassloaderContext();
    
    /**
     * Returns the default JspCompilerFactory object
     * 
     * @return JspCompilerFactory
     */
    JspCompilerFactory getDefaultJspCompilerFactory();
}
