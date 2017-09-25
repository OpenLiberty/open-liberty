/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.async.factory;

/**
 *
 */
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.async.AsyncContextFactory;
import com.ibm.ws.webcontainer31.async.AsyncContext31Impl;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

@Component(service = AsyncContextFactory.class, property = { "service.vendor=IBM" })
public class AsyncContextFactory40Impl implements AsyncContextFactory {

    @Override
    public AsyncContext getAsyncContext(IExtendedRequest iExtendedRequest, IExtendedResponse iExtendedResponse, IWebAppDispatcherContext webAppDispatcherContext) {
        return new AsyncContext31Impl(iExtendedRequest, iExtendedResponse, webAppDispatcherContext);
    }
}