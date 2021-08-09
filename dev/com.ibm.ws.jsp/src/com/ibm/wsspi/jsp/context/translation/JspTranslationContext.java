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
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;

/**
 * Implementations of this interface are used by the JSP Container to
 * provide access to external resources and also additional Factory implements for
 * Resource management and Compiler management. It extends the JspCoreContext 
 * interface to provide function specific to translating JSP's in addtion to those
 * found in JspCoreContext.
 */
public interface JspTranslationContext extends JspCoreContext {
    /**
     * Returns the JspResourcesFactory object that the JSP Container will 
     * use to create JSP Resource objects
     * 
     * @return JspResourcesFactory
     */
    JspResourcesFactory getJspResourcesFactory();
    
    /**
     * Returns the JspCompilerFactory object that the JSP Container will 
     * use to create JspCompiler objects.
     * 
     * @return JspCompilerFactory
     */
    JspCompilerFactory getJspCompilerFactory();
    
    /**
     * This method is called by the JSP Container to provide a JSP environment
     * object that can be used to obtain default version of the factories and other
     * useful utility functions.
     * 
     * @param jspEnvironment
     */
    void setJspTranslationEnviroment(JspTranslationEnvironment jspEnvironment);
    
}
