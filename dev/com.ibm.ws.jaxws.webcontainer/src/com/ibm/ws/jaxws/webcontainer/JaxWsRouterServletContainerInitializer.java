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
package com.ibm.ws.jaxws.webcontainer;

import java.util.ArrayList;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class JaxWsRouterServletContainerInitializer implements ServletContainerInitializer {

    private final static TraceComponent tc = Tr.register(JaxWsRouterServletContainerInitializer.class);

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext sc) throws ServletException {

        if (sc instanceof WebApp) {
            WebApp wapp = WebApp.class.cast(sc);
            String moduleName = wapp.getModuleMetaData().getName();

            //check if jaxws router module
            if (moduleName.indexOf("-Router") == -1) {
                return;
            }

            try {
                NonPersistentCache overlayCache = wapp.getModuleContainer().adapt(NonPersistentCache.class);
                JaxWsModuleInfo jaxwsModuleInfo = (JaxWsModuleInfo) overlayCache.getFromCache(JaxWsModuleInfo.class);

                if (jaxwsModuleInfo == null)
                    return;
            } catch (UnableToAdaptException e) {
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Jaxws EJB Router module '" + moduleName + "' is detected", moduleName);
            }

            //find out if there is jsf StartupServletContextListener
            ArrayList<ServletContextListener> sca = wapp.getServletContextListeners();
            int index = -1;
            for (int i = 0; i < sca.size(); i++) {
                ServletContextListener scl = sca.get(i);
                if (scl.getClass().getName().equals("org.apache.myfaces.webapp.StartupServletContextListener")) {
                    index = i;
                    break;
                }
            }

            //remove StartupServletContextListener as there is no need for router
            if (index != -1) {
                ServletContextListener scl = sca.remove(index);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "org.apache.myfaces.webapp.StartupServletContextListener is removed from Jaxws EJB Router module '" + moduleName + "'", moduleName, scl);
                }
            }
        }
    }
}
