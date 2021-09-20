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
package testservlet40.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Servlet 4.0: Context set/get request and response character encoding
 * Encoding already set to "KSC5601" in SCI inside app /lib/TestServlet40.jar
 *
 * Other request and response encoding tests are in TestEncoding.war
 *
 */

@WebServlet("/ServletEncoding")
public class ServletEncoding extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ServletEncoding() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String encoding = request.getCharacterEncoding();
        String display, contextEncoding;
        PrintWriter writer;

        contextEncoding = request.getHeader("Context-Encoding");
        if (contextEncoding != null) {
            ServletContext context = getServletContext();

            String reqEnc = context.getRequestCharacterEncoding();
            String resEnc = context.getResponseCharacterEncoding();
            boolean pass = true;

            if (!reqEnc.equals("KSC5601") || !resEnc.equals("KSC5601"))
                pass = false;

            display = "Test context getter char encoding. request enc [" + reqEnc + "], response enc [" + resEnc + "]";

            writer = response.getWriter();

            System.out.println(display);

            writer.println(display + (pass ? " PASS " : " FAIL "));

            try {
                //attempt to generate IllegalStateException
                context.setResponseCharacterEncoding("UTF-8");
            } catch (IllegalStateException ise) {
                writer.println("Caught the expected IllegalStateException when context.setRequestCharacterEncoding.  PASS");
                ise.printStackTrace(System.out);
            } catch (Exception e) {
                writer.println("Unexpected Exception. FAIL");
                e.printStackTrace(System.out);
            }

        } else {
            ServletOutputStream sos = response.getOutputStream();
            sos.println("Hello World. FAIL");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
