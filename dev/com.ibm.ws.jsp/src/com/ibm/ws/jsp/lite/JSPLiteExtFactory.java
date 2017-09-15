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
package com.ibm.ws.jsp.lite;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspFactory;

import org.apache.jasper.runtime.BodyContentImpl;
import org.apache.jasper.runtime.JspFactoryImpl;

import com.ibm.ws.jsp.Constants;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class JSPLiteExtFactory implements ExtensionFactory {
    public JSPLiteExtFactory() {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactoryImpl factory = new JspFactoryImpl(BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE);
            if (System.getSecurityManager() != null) {
                String basePackage = "org.apache.jasper.";
                try {
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspFactoryImpl$PrivilegedGetPageContext");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspFactoryImpl$PrivilegedReleasePageContext");
                    factory.getClass().getClassLoader().loadClass(basePackage + "runtime.JspRuntimeLibrary");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspRuntimeLibrary$PrivilegedIntrospectHelper");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.ServletResponseWrapperInclude");
                    //factory.getClass().getClassLoader().loadClass(basePackage + "servlet.JspServletWrapper");
                }
                catch (ClassNotFoundException ex) {
                    System.out.println("Jasper JspRuntimeContext preload of class failed: " + ex.getMessage());
                }
            }
            JspFactory.setDefaultFactory(factory);
        }
    }
    
    public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception {
         
        ExtensionProcessor extensionProcessor = null;
        extensionProcessor = new JSPLiteExtProcessor(webapp); 

        return extensionProcessor;
    }

    public List getPatternList() {
        ArrayList extensionsSupported = new ArrayList();

        for (int i = 0; i < Constants.STANDARD_JSP_EXTENSIONS.length; i++) {
            extensionsSupported.add(Constants.STANDARD_JSP_EXTENSIONS[i]);
        }

        return extensionsSupported;
    }
}
