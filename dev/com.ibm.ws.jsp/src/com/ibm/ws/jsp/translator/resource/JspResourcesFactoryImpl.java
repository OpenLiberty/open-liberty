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
package com.ibm.ws.jsp.translator.resource;

import javax.servlet.jsp.tagext.TagFileInfo;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.JspResources;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;

public class JspResourcesFactoryImpl implements JspResourcesFactory {
    protected JspOptions jspOptions = null;
    protected JspCoreContext context = null;
    protected Container container;
    
    public JspResourcesFactoryImpl(JspOptions jspOptions, JspCoreContext context, Container container) {
        this.jspOptions = jspOptions;
        this.context = context;    
        this.container = container;
    }
    
    public JspResources createJspResources(JspInputSource inputSource) {
        if (container!=null) {
            return new JspResourcesContainerImpl(inputSource, jspOptions, context);
        } else {
            return new JspResourcesImpl(inputSource, jspOptions, context);
        }
    }

    public TagFileResources createTagFileResources(JspInputSource inputSource, TagFileInfo tfi) {
        return new TagFileResourcesImpl(inputSource, tfi, jspOptions, context);
    }
}
