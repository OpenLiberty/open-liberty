/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.http.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Servlet class to get and print out in the response the Remote Address,
 * RemoteHost, RemotePort, Scheme and isSecure.
 */
@WebServlet("/EndpointInformationServlet")
public class EndpointInformationServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    public EndpointInformationServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setHeader("Content-Type", "text/plain");

        PrintWriter pw = res.getWriter();
        pw.print("Endpoint Information Servlet Test. ");
        String payload = "This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided. This message contains enough bytes for the autocompress tool to determine it is valid to compress. There must be 2048 bytes minimum for compression to happen. This message will be repeated until 2048 bytes are provided.";

        if (req.getHeader("TestCondition") != null) {
            String value = req.getHeader("TestCondition");

            if ("vary".equalsIgnoreCase(value)) {
                res.setHeader("Vary", "test");
            }

            else if ("smallSize".equalsIgnoreCase(value)) {
                payload = "";
                res.setHeader("Content-Length", "35");
            }

            else if ("regularSize".equalsIgnoreCase(value)) {
                res.setHeader("Content-Length", "2234");
            }

            else if ("testContentType".equalsIgnoreCase(value)) {
                res.setHeader("Content-Type", "test");
            }

            else if ("testContentEncoding".equalsIgnoreCase(value)) {
                //This text should not go through the channel's compression
                //code. Simulates custom servlet compression notified by the
                //Content-Encoding header.
                res.setHeader("Vary", "Accept-Encoding");
                res.setHeader("Content-Encoding", "gzip");
            }
        }

        pw.print(payload);

    }

}
