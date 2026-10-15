/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jsonp22.fat.web;

import java.io.IOException;

import io.openliberty.jsonp22.fat.checker.ClassLoaderRef;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Deployed inside BundledProviderApp. On any GET, warms the JsonProvider cache
 * for this WAR's classloader and registers that classloader in ClassLoaderRef so
 * that CacheCheckerServlet can observe whether it has been GC'd after undeploy.
 *
 * ClassLoaderRef is loaded by the gcTestLib shared library classloader (the common
 * parent of both WARs), so ClassLoaderRef.set() writes into the single shared copy
 * of the static field — visible to CacheCheckerServlet in the other WAR.
 */
@WebServlet("/ClassLoaderRegistrationServlet")
@SuppressWarnings("serial")
public class ClassLoaderRegistrationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Warm the provider cache for this WAR's classloader.
        JsonProvider.provider();

        // Register this WAR's classloader in the shared holder.
        // Liberty sets the TCCL to BundledProviderApp's classloader before
        // dispatching this request, so this captures the correct classloader.
        ClassLoaderRef.set(Thread.currentThread().getContextClassLoader());

        resp.setContentType("text/plain");
        resp.getWriter().print("registered");
    }
}
