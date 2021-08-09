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
 * The JspCompilerFactory interface provides a way of creating JspCompiler objects
 */
public interface JspCompilerFactory {
    /**
     * The method is called by the JSP Container when it has translated a JSP and
     * needs to compile the generated servlet.
     * 
     * @return JspCompiler object to be used to compile a generated JSP servlet.
     */
    JspCompiler createJspCompiler();
}
