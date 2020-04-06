/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ResponseReset")
public class ResponseReset extends HttpServlet {
    private static final long serialVersionUID = 1L;
    static final String param1 = "firstType";
    static final String pWriterType = "pWriter";
    static final String param2 = "secondType";
    static final String outputStreamType = "outputStream";

    public ResponseReset() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String type = request.getParameter(param1);
        if (pWriterType.equalsIgnoreCase(type)) {
            //getPrintWriter
            PrintWriter pWriter = response.getWriter();
            pWriter.println("Got first PrintWriter");
            pWriter.println("FAILURE");
        } else if (outputStreamType.equalsIgnoreCase(type)) {
            //getOutputStream
            ServletOutputStream sos = response.getOutputStream();
            sos.println("Got first ServletOutputStream");
            sos.println("FAILURE");
        } else {
            //report test case exception
            ServletOutputStream sos = response.getOutputStream();
            sos.println("FAILURE - test case exception: no first type specified");
        }
        response.reset();
        type = request.getParameter(param2);
        if (pWriterType.equalsIgnoreCase(type)) {
            //getPrintWriter
            PrintWriter pWriter = response.getWriter();
            pWriter.println("Got second PrintWriter");
            pWriter.println("SUCCESS");
        } else if (outputStreamType.equalsIgnoreCase(type)) {
            //getOutputStream
            ServletOutputStream sos = response.getOutputStream();
            sos.println("Got second ServletOutputStream");
            sos.println("SUCCESS");
        } else {
            //report test case exception
            ServletOutputStream sos = response.getOutputStream();
            sos.println("FAILURE - test case exception: no second type specified");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
