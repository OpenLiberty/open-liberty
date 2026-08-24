/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package netty.servlet.request.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = "/NettyServletRequestTestServlet")
public class NettyServletRequestTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream in = req.getInputStream();

        int i = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ((i = in.read()) != -1) {
            System.out.println("PAN: writing: " + i);
            out.write(i);
            System.out.println("PAN: about to read again");
        }

        System.out.println("PAN: about to get the response writer");
        PrintWriter writer = new PrintWriter(resp.getWriter());

        System.out.println("PAN: writing out the response data");
        writer.write("Chunk Data: " + new String(out.toByteArray()));
        writer.println();
        writer.write("isTrailerFieldsReady: " + req.isTrailerFieldsReady());
        writer.println();
        writer.write("Trailer: " + req.getTrailerFields().toString());

        System.out.println("PAN: about to flush the response");
        writer.flush();
    }

}
