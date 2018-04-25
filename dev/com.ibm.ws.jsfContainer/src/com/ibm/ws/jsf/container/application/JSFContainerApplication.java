/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.application;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ViewHandler;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.jsf.container.cdi.IBMViewHandlerProxy;

public class JSFContainerApplication extends ApplicationWrapper {

    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.container.application");
    private Application delegate;
    private String appname;

    public JSFContainerApplication(Application delegate, String appname) {
        this.delegate = delegate;
        this.appname = appname;
    }

    @Override
    public void setViewHandler(ViewHandler handler) {
        try {
            BeanManager beanManager = InitialContext.doLookup("java:comp/BeanManager");
            if (beanManager != null) {
                if (log.isLoggable(Level.FINEST)) {
                    log.logp(Level.FINEST, JSFContainerApplication.class.getName(), "setViewHandler", "Setting IBM View Handler");
                }

                delegate.setViewHandler(new IBMViewHandlerProxy(handler, appname));

            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.logp(Level.FINEST, JSFContainerApplication.class.getName(), "setViewHandler", "Setting default View Handler", appname);
                }

                delegate.setViewHandler(handler);
            }
        } catch (NamingException e) {
            delegate.setViewHandler(handler);
            if (log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, e.getMessage(), e);
        }
    }

    @Override
    public ViewHandler getViewHandler() {
        return delegate.getViewHandler();
    }

    @Override
    public Application getWrapped() {
        return delegate;
    }
}
