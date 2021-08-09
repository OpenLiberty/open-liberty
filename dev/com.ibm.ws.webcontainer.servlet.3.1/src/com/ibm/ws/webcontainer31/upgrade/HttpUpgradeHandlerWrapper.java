/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 *
 */
public class HttpUpgradeHandlerWrapper implements HttpUpgradeHandler {

    private final static TraceComponent tc = Tr.register(HttpUpgradeHandlerWrapper.class, WebContainerConstants.TR_GROUP, LoggerFactory.MESSAGES);

    HttpUpgradeHandler wrappedHandler;
    WebApp webapp;

    public HttpUpgradeHandlerWrapper(WebApp webapp, HttpUpgradeHandler handler) {
        this.webapp = webapp;
        wrappedHandler = handler;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpUpgradeHandler#destroy()
     */
    @Override
    public void destroy() {
        //call predestroy
        try {
            webapp.performPreDestroy(wrappedHandler);
        } catch (InjectionException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroy  injectionException during preDestroy: ", e);
            }
        }
        wrappedHandler.destroy();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpUpgradeHandler#init(javax.servlet.http.WebConnection)
     */
    @Override
    public void init(WebConnection connection) {
        wrappedHandler.init(connection);
    }

}
