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
package com.ibm.wsspi.jsp.resource;

import java.net.URL;

/**
 * This factory is used by the JSP Container to create JspInputSource objects
 */
public interface JspInputSourceFactory {
    /**
     * Returns a JspInputSource object given a relative url
     * @param relativeURL
     * @return JspInputSource
     */
    JspInputSource createJspInputSource(String relativeURL);
    
    /**
     * Returns a JspInputSource object given a relative URL and 
     * an alternative context URL
     * @param contextURL
     * @param relativeURL
     * @return JspInputSource
     */
    JspInputSource createJspInputSource(URL contextURL, String relativeURL);
    
    /**
     * Returns a new JspInputSource object that has it context information obtained
     * from the provided base input source. The relative URL is used to provide the
     * addtional information.
     * 
     * @param base
     * @param relativeURL
     * @return JspInputSource
     */
    JspInputSource copyJspInputSource(JspInputSource base, String relativeURL);
}
