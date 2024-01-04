/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.async.factory;

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
public class AsyncContextFactory61Impl implements AsyncContextFactory {

    @Override
    public AsyncContext getAsyncContext(IExtendedRequest iExtendedRequest, IExtendedResponse iExtendedResponse, IWebAppDispatcherContext webAppDispatcherContext) {
        return new AsyncContext31Impl(iExtendedRequest, iExtendedResponse, webAppDispatcherContext);
    }
}
