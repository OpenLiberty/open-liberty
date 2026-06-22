/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package acceptlanguage.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Accept-Language header and Locales handling.
 * Logs the Accept-Language header and its length locally.
 *
 * Returns the header length and total processed (valid) locales in the response.

 * NOTE: There will be 2000+ requests, so limit the LOG if possible
 */
@WebServlet(urlPatterns = {"/TestAcceptLanguage"}, name = "AcceptLanguageTestServlet")
public class AcceptLanguageTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = AcceptLanguageTestServlet.class.getName();
    private static final boolean TRACEON = false;        //turn ON for debug; production OFF

    public AcceptLanguageTestServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String testName = request.getHeader("runTest");

        String temp = null;
        String acceptLanguage = request.getHeader("Accept-Language");
        int length = (acceptLanguage != null) ? acceptLanguage.length() : 0;

        if (TRACEON) {
            LOG("runTest " + testName);
            LOG("Accept-Language header: [" + acceptLanguage + "]");
            LOG("Accept-Language length: " + length);
        }

        int localeCount = 0;
        List<String> localeValues = new ArrayList<>();
        try {
            Enumeration<Locale> locales = request.getLocales();

            while (locales.hasMoreElements()) {
                Locale locale = locales.nextElement();
                String localeValue = locale.toString();

                if (TRACEON)
                    LOG("Locale: " + localeCount + " ; localeValue: " + localeValue);

                localeValues.add(localeValue);
                localeCount++;
            }
        } catch (Exception e) {
            temp = e.getMessage();
            LOG("Exception during getLocales(): " + temp);
        }

        response.setContentType("text/plain");

        if (temp == null) //response with exception otherwise
            temp = "Accept-Language length [" + length + "] , Locales ["+ localeCount + "] Locale values: " + String.join(",", localeValues);

        if (TRACEON)
            LOG("Response: " + temp);

        response.getOutputStream().println(temp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}