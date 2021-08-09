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
package com.ibm.wsspi.jsp.compiler;

import java.util.Collection;
import java.util.List;

import com.ibm.wsspi.jsp.resource.translation.JspResources;

/**
 * The JspCompiler interface provides support for a pluggable Java Compiler.
 */
public interface JspCompiler {
    /**
     * The compile method is called by the JSP Container to compile a generated JSP servlet.
     * 
     * @param jspResources An array JspResources object containing the paths to the source files
     * @param jspResources An array JspResources object containing the dependencies
     * @param jspLineIds A collection of JspLineId objects that can be used to lookup JSP source line numbers 
     * @param compilerOptions  A List of String objects to be passed on the java compiler command-line  
     * @return JspCompilerResult - Contains details of any compile failure and relevant messages
     */
    public JspCompilerResult compile(JspResources[] jspResources, JspResources[] dependencyResources, Collection jspLineIds, List compilerOptions); 
    /**
     * The compile method is called by the JSP Container to compile a generated JSP servlet.
     * 
     * @param sourcePath A string containing the path to the source file
     * @param jspLineIds A collection of JspLineId objects that can be used to lookup JSP source line numbers 
     * @param compilerOptions  A List of String objects to be passed on the java compiler command-line  
     * @return JspCompilerResult - Contains details of any compile failure and relevant messages
     * 
     */
    public JspCompilerResult compile(String sourcePath, Collection jspLineIds, List compilerOptions); 
}
