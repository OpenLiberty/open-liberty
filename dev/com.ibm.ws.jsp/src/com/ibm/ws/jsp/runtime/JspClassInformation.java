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
package com.ibm.ws.jsp.runtime;

/**
 * Interface for retrieving information about the classfile.
 */

public interface JspClassInformation {

   /**
    * Returns a list of files names that the current page has a source
    * dependency on for the purpose of compiling out of date pages.  This is used for
    * 1) files that are included by include directives
    * 2) files that are included by include-prelude and include-coda in jsp:config
    * 3) files that are tag files and referenced
    * 4) TLDs referenced
    */
    public String[] getDependants();
    /**
     * Returns the WebSphere version on which the JSP classfile was generated
     */
    public String getVersionInformation();
 
	// begin 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.
    /**
     * Returns whether the JSP was compiled with debug enabled.
     */
    public boolean isDebugClassFile ();
	// end 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.
    
}