/*******************************************************************************
 * Copyright (c) 1997, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class JSPExtensionServletWrapper extends AbstractJSPExtensionServletWrapper {
    
    public JSPExtensionServletWrapper(IServletContext parent, 
                                      JspOptions options, 
                                      JspConfigurationManager configManager, 
                                      TagLibraryCache tlc,
                                      JspTranslationContext context, 
                                      CodeSource codeSource) throws Exception {
        super(parent, options, configManager, tlc, context, codeSource);
    }

    protected PermissionCollection createPermissionCollection() throws MalformedURLException {
        return Policy.getPolicy().getPermissions(getCodeSource());
    }
    
    private CodeSource getCodeSource() {
        CodeSource topLevelContainerCodeSource = getTopLevelContainerCodeSource(context.getModuleContainer());
        return topLevelContainerCodeSource != null ? topLevelContainerCodeSource : codeSource;
    }
    
    private CodeSource getTopLevelContainerCodeSource(Container container) {
        CodeSource topLevelCodeSource = null;
        Container topLevelContainer = getTopLevelContainerContainer(container);
        Iterator<URL> urlsIterator = topLevelContainer.getURLs().iterator();
        if (urlsIterator.hasNext()) {
            topLevelCodeSource = new CodeSource(urlsIterator.next(), (java.security.cert.Certificate[]) null);
        }
        return topLevelCodeSource;
    }

    private Container getTopLevelContainerContainer(Container container) {
        Container topLevelContainer = container;
        Container enclosingContainer = container.getEnclosingContainer();
        if (enclosingContainer != null) {
            topLevelContainer = getTopLevelContainerContainer(enclosingContainer);
        }
        return topLevelContainer;
    }
    
    protected void preinvokeCheckForTranslation(HttpServletRequest req) throws JspCoreException {
    }
}
