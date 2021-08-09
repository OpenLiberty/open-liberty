/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp;

//import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.el.ExpressionFactory;
import javax.servlet.jsp.JspFactory;

import org.apache.jasper.runtime.BodyContentImpl;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public abstract class JspShim {

    public static ExpressionFactory createWrappedExpressionFactory(ExpressionFactory expressionFactory) {
        // tWAS
        //return new org.apache.webbeans.el.WrappedExpressionFactory(expressionFactory);
        // lWAS
        throw new IllegalStateException("no JCDI support");
    }

    public static URL getGlobalTagLibraryCacheContextURL() throws MalformedURLException {
        // tWAS
        //String installRoot =
        //    java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
        //        public String run() {
        //            return System.getProperty("was.install.root");
        //        }
        //    });
        //String root = new File(installRoot + File.separator + "lib").getPath();
        //return new URL("file", null, root + "/");
        // lWAS
        //we need to use a location manager, in an ideal world we would use DS to have it injected
        //but this code is static and the createGlobalTagLibraryCache() is new'd up in places without
        //being a DS component, so we don't really have any choice but to manually look up the service

        //get our bundle
        Bundle jspBundle = FrameworkUtil.getBundle(JspShim.class);
        if (jspBundle != null) {
            //as long as we were in an OSGi environment and got a bundle
            //get the bundle context
            BundleContext ctx = jspBundle.getBundleContext();
            //get a reference to the location manager service
            ServiceReference<WsLocationAdmin> locMgrRef = ctx.getServiceReference(WsLocationAdmin.class);
            //go through the process of getting a service
            //resolve the reference
            //unget the service
            //make sure we check null at each opportunity
            if (locMgrRef != null) {
                WsLocationAdmin locMgr = ctx.getService(locMgrRef);
                if (locMgr != null) {
                    WsResource res = locMgr.resolveResource(WsLocationConstants.SYMBOL_SHARED_APPS_DIR + "webcontainer/");
                    ctx.ungetService(locMgrRef);
                    if (res != null) {
                        return res.toExternalURI().toURL();
                    }
                }
            }
        }
        return null;

    }

    public static void setDefaultJspFactory() {
        // tWAS
        // nothing, done in WASJSPExtensionFactory
        // lWAS
        if (JspFactory.getDefaultFactory() == null) {
            JspFactoryImpl factory = new JspFactoryImpl(BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE);
            JspFactory.setDefaultFactory(factory);
        }
    }

}
