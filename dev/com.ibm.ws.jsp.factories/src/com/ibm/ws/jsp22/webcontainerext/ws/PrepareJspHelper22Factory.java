/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp22.webcontainerext.ws;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelper;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelperFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * This factory creates instances of the PrepareJspHelper class that are
 * specific to the JSP 2.2 spec.
 */
@Component(property = { "service.vendor=IBM" })
public class PrepareJspHelper22Factory implements PrepareJspHelperFactory {

    /* (non-Javadoc)
     * @see com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelperFactory#createPrepareJspHelper(com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor, com.ibm.wsspi.webcontainer.servlet.IServletContext, com.ibm.ws.jsp.JspOptions)
     */
    @Override
    public PrepareJspHelper createPrepareJspHelper(AbstractJSPExtensionProcessor s, IServletContext webapp, JspOptions options) {
        return new PrepareJspHelper(s, webapp, options);
    }

}
