/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.cdi;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class CDIJSFInitializer {

    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.container.cdi");

    public static void initialize(Application application, String appname) {
        try {
            BeanManager beanManager = InitialContext.doLookup("java:comp/BeanManager");
            if (beanManager != null) {
                if (log.isLoggable(Level.FINEST))
                    log.logp(Level.FINEST, CDIJSFInitializer.class.getName(), "initializeJSF",
                             "Initializing application with CDI", appname);

                /*
                 * Load WeldELContextListener reflectively. This is necessary since the package
                 * changed from org.jboss.weld.el -> org.jboss.weld.module.web.el in Weld 3.0.
                 */
                try {
                    String className;

                    if ("2.2".equals(System.getProperty("com.ibm.ws.jsfContainer.JSF_SPEC_LEVEL")))
                        className = "org.jboss.weld.el.WeldELContextListener";
                    else
                        className = "org.jboss.weld.module.web.el.WeldELContextListener";

                    Constructor<?> constructor = Class.forName(className).getConstructor();
                    application.addELContextListener((ELContextListener) constructor.newInstance());
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }

                ELResolver elResolver = beanManager.getELResolver();
                application.addELResolver(elResolver);
            } else {
                if (log.isLoggable(Level.FINEST))
                    log.logp(Level.FINEST, CDIJSFInitializer.class.getName(), "initializeJSF",
                             "No BeanManager found for application", appname);
            }
        } catch (NamingException e) {
            if (log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, e.getMessage(), e);
        }
    }
}
