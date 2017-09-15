/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.inmemory.resource;

import javax.servlet.jsp.tagext.TagFileInfo;

import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.JspResources;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;

public class InMemoryJspResourceFactory implements JspResourcesFactory {
    private JspCoreContext context = null; 
    private JspTranslationEnvironment env = null;

    public InMemoryJspResourceFactory(JspCoreContext context, JspTranslationEnvironment env) {
        this.context = context;
        this.env = env;
    }
    
    public JspResources createJspResources(JspInputSource jspInputSource) {
        return new InMemoryJspResources(jspInputSource, context, env);
    }

    public TagFileResources createTagFileResources(JspInputSource tagFileInputSource, TagFileInfo tagFileInfo) {
        return new InMemoryTagFileResources(tagFileInputSource, tagFileInfo, context, env);
    }
}
