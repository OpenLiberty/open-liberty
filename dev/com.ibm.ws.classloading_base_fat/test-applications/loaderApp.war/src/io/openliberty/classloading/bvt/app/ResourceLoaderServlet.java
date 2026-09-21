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
package io.openliberty.classloading.bvt.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used by the classloading BVT FAT tests to:
 * <ul>
 * <li>Load a named resource via the classloader ({@code resource} param)</li>
 * <li>Attempt to load a class by name ({@code classname} param alone)</li>
 * <li>Retrieve {@code Class.getResource()} URLs ({@code classname} + {@code resource} params together)</li>
 * <li>Retrieve {@code ClassLoader.getResources()} URLs ({@code classloaderResource} param)</li>
 * </ul>
 */
@WebServlet("/")
@SuppressWarnings("serial")
public class ResourceLoaderServlet extends HttpServlet {

    private void doResourceLoad(String resource, PrintWriter writer) throws IOException {
        try {
            writer.print(ResourceLoader.getResource(resource, ResourceLoaderServlet.class.getClassLoader()));
        } catch (Exception e) {
            writer.print("EXCEPTION: " + e.getMessage());
        }
    }

    private void doClassLoad(String classname, PrintWriter writer) {
        try {
            Class<?> test = Class.forName(classname);
            if (test != null) {
                writer.println("SUCCESS");
            }
        } catch (Exception e) {
            writer.println("FAILURE");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        String resource = request.getParameter("resource");
        String classname = request.getParameter("classname");
        String classloaderResource = request.getParameter("classloaderResource");

        if (resource != null && classname != null) {
            doClassResourceURLs(classname, resource, writer);
            return;
        }

        if (classloaderResource != null) {
            doClassloaderResourceURLs(classloaderResource, writer);
        }

        if (resource != null)
            doResourceLoad(resource, writer);

        if (classname != null)
            doClassLoad(classname, writer);
    }

    private void doClassResourceURLs(String classname, String resource, PrintWriter writer) {
        try {
            Class<?> test = getClass();
            if (classname.length() > 0)
                test = Class.forName(classname);
            if (test != null) {
                writer.println(test.getResource(resource));
            }
        } catch (Exception e) {
            writer.println("FAILURE");
        }
    }

    private void doClassloaderResourceURLs(String resource, PrintWriter writer) {
        try {
            Enumeration<URL> urls = getClass().getClassLoader().getResources(resource);
            while (urls.hasMoreElements()) {
                writer.println(urls.nextElement());
            }
        } catch (Exception e) {
            writer.println("FAILURE");
        }
    }

}
