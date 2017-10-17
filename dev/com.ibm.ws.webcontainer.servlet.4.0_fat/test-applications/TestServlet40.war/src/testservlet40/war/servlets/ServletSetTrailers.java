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
package testservlet40.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/ServletSetTrailers")
public class ServletSetTrailers extends HttpServlet {

    HashMap<String, String> trailers = new HashMap<String, String>();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter pw = response.getWriter();

        String test = request.getParameter("Test");

        pw.println("ServletSetTrailers : Test = " + test);

        Supplier<Map<String, String>> responseTrailers = new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                return trailers;
            }

        };

        response.setHeader("Trailer", "MyTrailer1, MyTrailer2");

        if (test != null && test.equals("Add1Trailer")) {

            response.setHeader("Trailer", "MyTrailer1");
            addTrailer(responseTrailers, pw, "MyTrailer1", "MyTrailerValue1");
            try {
                response.setTrailerFields(responseTrailers);
                pw.println("PASS : Trailers set for the response.");
            } catch (IllegalStateException ise) {
                pw.println("FAIL : IllegalStateException thrown when setTrailerFields is called before commit.");
            }

        } else if (test != null && test.equals("Add2Trailers")) {

            response.setHeader("Trailer", "MyTrailer1, MyTrailer2");
            addTrailer(responseTrailers, pw, "MyTrailer1", "MyTrailerValue1");
            addTrailer(responseTrailers, pw, "MyTrailer2", "MyTrailerValue2");
            try {
                response.setTrailerFields(responseTrailers);
                pw.println("PASS : Trailers set for the response.");
            } catch (IllegalStateException ise) {
                pw.println("FAIL : IllegalStateException thrown when setTrailerFields is called before commit.");
            }

        } else if (test != null && test.equals("Add3Trailers")) {

            response.setHeader("Trailer", "MyTrailer1, MyTrailer2, MyTrailer3");
            addTrailer(responseTrailers, pw, "MyTrailer1", "MyTrailerValue1");
            addTrailer(responseTrailers, pw, "MyTrailer2", "MyTrailerValue2");
            try {
                response.setTrailerFields(responseTrailers);
                pw.println("PASS : Trailers set for the response.");
            } catch (IllegalStateException ise) {
                pw.println("FAIL : IllegalStateException thrown when setTrailerFields is called before commit.");
            }
            addTrailer(responseTrailers, pw, "MyTrailer3", "MyTrailerValue3");

        } else {
            pw.flush();
            addTrailer(responseTrailers, pw, "MyTrailer1", "MyTrailerValue1");
            try {
                response.setTrailerFields(responseTrailers);
                pw.println("FAIL : IllegalStateException not thrown when setTrailerFields is called after commit");
            } catch (IllegalStateException ise) {
                pw.println("PASS : IllegalStateException thrown when setTrailerFields is called after commit");
            }

        }

    }

    private void addTrailer(Supplier<Map<String, String>> trailerSupplier, PrintWriter pw, String name, String value) {

        Map<String, String> trailerMap = trailerSupplier.get();

        trailerMap.put(name, value);

        pw.println("Trailer field added : " + name + " " + value);

    }

}
