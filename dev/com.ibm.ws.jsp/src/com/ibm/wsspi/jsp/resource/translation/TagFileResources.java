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
package com.ibm.wsspi.jsp.resource.translation;

/**
 * Implementions of this interface are use by the JSP Container to handle Tag File Resources 
 * such as the tag file input source, the generated source file and the class name used for the
 * generated servlet. It is also used by the JSP container to check if a tag file is outdated and
 * syncronize the resources if a translation occurs. 
 */
public interface TagFileResources extends JspResources {
    /**
     * Called by the JSP Container to synchronize the tag file generated source files.
     */
    void syncGeneratedSource();
}
