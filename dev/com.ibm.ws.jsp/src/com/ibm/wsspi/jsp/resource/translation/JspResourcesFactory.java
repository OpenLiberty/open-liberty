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

import javax.servlet.jsp.tagext.TagFileInfo;

import com.ibm.wsspi.jsp.resource.JspInputSource;

/**
 * Used by the JSP Container to create jsp and tagfile resource objects
 */
public interface JspResourcesFactory {
    /**
     * Returns a JspResouces object for the given JspInputSource.
     * 
     * @param jspInputSource
     * @return JspResources
     */
    JspResources createJspResources(JspInputSource jspInputSource);
    
    /**
     * Returns a TagFileResouces object for the given TagFile Input Source and 
     * TagFileInfo object representing the tagfile.
     * 
     * @param tagFileInputSource
     * @param tagFileInfo
     * @return TagFileResources
     */
    TagFileResources createTagFileResources(JspInputSource tagFileInputSource, TagFileInfo tagFileInfo);
}
