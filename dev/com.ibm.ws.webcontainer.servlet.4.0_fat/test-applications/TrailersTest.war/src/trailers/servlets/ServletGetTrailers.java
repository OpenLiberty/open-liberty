/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package trailers.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import trailers.listeners.ReadListenerGetTrailers;

/**
 *
 */
@WebServlet(urlPatterns = "/ServletGetTrailers", asyncSupported = true)
public class ServletGetTrailers extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    private final HashMap<String, String> tailerMap = new HashMap<String, String>();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter pw = response.getWriter();

        String test = request.getParameter("Test");

        String netty = request.getParameter("usingNetty");

        pw.println("ServletGetTrailers : Test = " + test);
        pw.println("ServletGetTrailers : usingNetty = " + netty);

        if (test != null && test.equals("RL")) {
            AsyncContext ac = request.startAsync();
            ServletInputStream input = request.getInputStream();
            ReadListener readListener = new ReadListenerGetTrailers(input, response, ac, request, Boolean.parseBoolean(netty));
            input.setReadListener(readListener);

        } else {
            // If using Netty, we will always have the trailers available

            if(!Boolean.parseBoolean(netty)){
                if (request.isTrailerFieldsReady()) {
                    pw.println("FAIL : isTrailerFieldsReady() returned true before data was read.");
                } else {
                    pw.println("PASS : isTrailerFieldsReady() returned false before data was read.");
                    try {
                        request.getTrailerFields();
                        pw.println("FAIL : getTrailerFields() did not throw IllegalStateException before data was read.");
                    } catch (IllegalStateException ise) {
                        pw.println("PASS : getTrailerFields() threw IllegalStateException before data was read.");
                    }
                }
            } else if(!request.isTrailerFieldsReady()){
                pw.println("FAIL : isTrailerFieldsReady() returned false while using Netty.");
            }

            

            ServletInputStream inStream = request.getInputStream();
            int len = -1;
            byte[] postData = new byte[128];
            while ((len = inStream.read(postData)) != -1) {
                pw.println("Post data :" + new String(postData, 0, len));
            } ;

            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                pw.println("Header : " + headerName + " = " + request.getHeader(headerName));
            }

            if (request.isTrailerFieldsReady()) {
                pw.println("PASS : isTrailerFieldsReady() returned true after data was read.");
                try {
                    Map<String, String> trailers = request.getTrailerFields();
                    pw.println("PASS : getTrailerFields() did not throw IllegalStateException before data was read.");

                    Iterator<String> trailerIterator = trailers.keySet().iterator();

                    while (trailerIterator.hasNext()) {
                        String trailerName = trailerIterator.next();
                        pw.println("Trailer field found :  " + trailerName + " = " + trailers.get(trailerName));
                    }

                } catch (IllegalStateException ise) {
                    pw.println("FAIL : isTrailerFieldsReady() theow IllegalStateException before data was read.");
                }

            } else {
                pw.println("FAIL : isTrailerFieldsReady() returned false after data was read.");
            }
        }
    }

    private void addTrailer(Supplier<Map<String, String>> trailerSupplier, PrintWriter pw, String name, String value) {

        Map<String, String> trailerMap = trailerSupplier.get();

        trailerMap.put(name, value);

        pw.println("Response Trailer field added : " + name + " " + value);

    }

}
