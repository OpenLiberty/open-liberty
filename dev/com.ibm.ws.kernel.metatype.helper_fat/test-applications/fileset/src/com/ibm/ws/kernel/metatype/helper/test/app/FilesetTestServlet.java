/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.metatype.helper.test.app;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.config.Fileset;

public class FilesetTestServlet extends HttpServlet {

    private static final long serialVersionUID = 2268417347748420049L;

    // Hacky way to get a BundleContext so we can check and use services... ick!
    BundleContext context = FrameworkUtil.getBundle(Servlet.class).getBundleContext();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        writer.println("This is Fileset servlet.");

        String test = req.getQueryString();

        try {
            if (test != null) {
                if (test.equals("Service")) {
                    testFilesetServiceRegistered(writer);
                } else if (test.equals("Files")) {
                    printFilesetFiles(writer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(writer);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private void testFilesetServiceRegistered(PrintWriter writer) throws Exception {
        Collection<ServiceReference<Fileset>> filesets = context.getServiceReferences(Fileset.class, "(id=testFileset)");

        // output how many filesets we found
        writer.println("Found " + filesets.size() + " fileset service.");
    }

    private void printFilesetFiles(PrintWriter writer) throws Exception {
        Collection<ServiceReference<Fileset>> filesets = context.getServiceReferences(Fileset.class, "(id=testFileset)");
        for (ServiceReference<Fileset> filesetRef : filesets) {
            Fileset fset = null;
            try {
                fset = context.getService(filesetRef);
                Collection<File> files = fset.getFileset();
                ArrayList<String> sortedFileNames = new ArrayList<String>(files.size());
                for (File f : files) {
                    sortedFileNames.add(f.getName());
                }
                Collections.sort(sortedFileNames);
                for (String name : sortedFileNames) {
                    writer.println("Fileset contained file: " + name);
                }
            } finally {
                if (fset != null)
                    context.ungetService(filesetRef);
            }
        }
    }
}
