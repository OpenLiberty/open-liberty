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
package com.ibm.ws.cdi.web.impl;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * WeldInitiallAsyncListener is registered as the first AsyncListener
 * so that is is called before all others. It's role is to add the request
 * context to the thread for use by other AsyncListenets. The request
 * request context will be removed by the WeldTerminallAsyncListener.
 */
public class WeldInitialAsyncListener implements AsyncListener {

    private static final TraceComponent tc = Tr.register(WeldInitialAsyncListener.class);

    private final ServletRequestListener weldListener;
    private final ServletContext sc;

    public WeldInitialAsyncListener(ServletRequestListener weldlistener, ServletContext sc) {
        this.weldListener = weldlistener;
        this.sc = sc;
    }

    /** {@inheritDoc} */
    @Override
    public void onComplete(AsyncEvent asyncEvent) {

        notifyWeldInitialListener((HttpServletRequest) asyncEvent.getSuppliedRequest());

    }

    /** {@inheritDoc} */
    @Override
    public void onError(AsyncEvent asyncEvent) throws IOException {

        notifyWeldInitialListener((HttpServletRequest) asyncEvent.getSuppliedRequest());

    }

    /** {@inheritDoc} */
    @Override
    public void onStartAsync(AsyncEvent asyncEvent) throws IOException {

        // re-register the listener because starting async removes listeners.
        asyncEvent.getAsyncContext().addListener(this, asyncEvent.getSuppliedRequest(), asyncEvent.getSuppliedResponse());

    }

    /** {@inheritDoc} */
    @Override
    public void onTimeout(AsyncEvent asyncEvent) throws IOException {

        notifyWeldInitialListener((HttpServletRequest) asyncEvent.getSuppliedRequest());

    }

    private void notifyWeldInitialListener(HttpServletRequest req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " call weldListener.requestInitialized() req =" + req + ", sc = " + sc);
        ServletRequestEvent servletRequestEvent = new ServletRequestEvent(sc, req);
        weldListener.requestInitialized(servletRequestEvent);
    }

}
