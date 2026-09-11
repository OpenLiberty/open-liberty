/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package test.delegation.bundled;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;

public class GetTempDirectoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public GetTempDirectoryServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logClassResourceURL(FileUtils.class, "org/apache/commons/io/FileUtils.class");
        logClassResourceURL(MDC.class, "org/slf4j/MDC.class");

        File tempdir = FileUtils.getTempDirectory();

        response.setStatus(HttpServletResponse.SC_OK);

        new FileExistsException();
        Writer writer = response.getWriter();
        writer.write("<html><body><br />");
        writer.write("<p>FileUtils.getTempDirectory(): <span style='color:blue'>");
        writer.write(tempdir.getPath());
        writer.write("</span></p>");
        writer.write("<p>FileUtils.class: <span style='color:blue'>");
        writer.write(FileUtils.class.getClassLoader().getResource("org/apache/commons/io/FileUtils.class").toExternalForm());
        writer.write("</span></p>");
        writer.write("</body></html>");

        writer.flush();
    }

    private void logClassResourceURL(Class<?> cls, String path) {
        URL url = cls.getClassLoader().getResource(path);
        System.out.println("[" + this.getClass().getName() + "] " + cls.getName() + " => " + url.toExternalForm());
    }
}
