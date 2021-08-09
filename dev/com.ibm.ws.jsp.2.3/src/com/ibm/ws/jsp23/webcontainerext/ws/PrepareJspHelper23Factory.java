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
package com.ibm.ws.jsp23.webcontainerext.ws;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelper;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelperFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 */
@Component(property = { "service.vendor=IBM" })
public class PrepareJspHelper23Factory implements PrepareJspHelperFactory {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.osgi.webapp.WebAppFactory#createWebApp(com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration, java.lang.ClassLoader,
     * com.ibm.wsspi.injectionengine.ReferenceContext, com.ibm.ws.container.service.metadata.MetaDataService, com.ibm.websphere.csi.J2EENameFactory)
     */
    @Override
    public PrepareJspHelper createPrepareJspHelper(AbstractJSPExtensionProcessor s, IServletContext webapp, JspOptions options) {
        return new PrepareJspHelper23(s, webapp, options);
    }

}
