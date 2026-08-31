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
package io.openliberty.jsonp22.fat.checker;

import java.io.IOException;
import java.lang.ref.WeakReference;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Always-present servlet that reads the WeakReference stored in ClassLoaderRef
 * (a shared library class) and reports whether BundledProviderApp's classloader
 * has been GC'd.
 *
 *   GET /CacheCheckerServlet  — returns "null" if CL was GC'd,
 *                               "never-registered" if not yet set,
 *                               or the CL's toString() if still alive.
 */
@WebServlet("/CacheCheckerServlet")
@SuppressWarnings("serial")
public class CacheCheckerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain");

        WeakReference<ClassLoader> ref = ClassLoaderRef.get();
        if (ref == null) {
            resp.getWriter().print("never-registered");
            return;
        }
        System.gc(); // Best-effort hint to the server JVM's garbage collector.
        ClassLoader cl = ref.get();
        resp.getWriter().print(cl == null ? "null" : cl.toString());
    }
}
