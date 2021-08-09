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

/**
 * The JspCompilerResult interface provides a repository for the results of a JSP servlet compile
 */
public interface JspCompilerResult {
    /**
     * Returns the return code for the java compile
     * @return int java compile return code. Non-zero is treated as a compile failure
     */
    int getCompilerReturnValue();
    
    /**
     * Returns any messages that the java compiler produced. When a compile fails this method is called
     * to provide the error message that is passed back to the caller of the JSP.
     * @return
     */
    String getCompilerMessage();

    //PK72039
    /**
     * Returns a List of JSPs which failed to compile in a directory during batch compilation.
     * @return List
     */
    java.util.List getCompilerFailureFileNames();
}
