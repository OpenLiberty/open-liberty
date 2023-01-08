/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test HttpServletRequest.getHttpServletMapping() info for dispatch Async type
 * Test request attributes return correctly
 *
 */
@WebServlet(urlPatterns = "/ServletMapping", name = "ServletMapping", loadOnStartup = 2)
public class ServletMapping extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = ServletMapping.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public ServletMapping() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test ServletMapping dispatched from an async servlet.");

        boolean testPass = true;
        StringBuilder sBuilderResponse = new StringBuilder("TEST ServletMapping for Async type . Message [[[-- ");
        StringBuilder sbFound = new StringBuilder();
        String result;

        final String expectedResult = "uriPath=ServletMapping, urlPattern=/ServletMapping, servletName=ServletMapping, mappingMatch=EXACT";
        final String expectedAttributesResult = "ASYNC_REQUEST_URI=/GetMappingAsyncDispatchTest/AsyncServlet, ASYNC_CONTEXT_PATH=/GetMappingAsyncDispatchTest, ASYNC_SERVLET_PATH=/AsyncServlet, ASYNC_PATH_INFO=null, ASYNC_QUERY_STRING=null, ASYNC_MAPPING="
                                                + request.getAttribute(AsyncServlet.ORIGINAL_MAPPING_ATT);

        //Test servletMapping data
        HttpServletMapping mapping = request.getHttpServletMapping();
        if (mapping == null) {
            result = "mapping is null";
        } else {
            sbFound.append("uriPath=" + mapping.getMatchValue())
                            .append(", urlPattern=" + mapping.getPattern())
                            .append(", servletName=" + mapping.getServletName())
                            .append(", mappingMatch=" + mapping.getMappingMatch());

            result = sbFound.toString();
        }

        LOG.info(" found [" + result + "]");

        if (expectedResult.equals(result)) {
            sBuilderResponse.append("Test ServletMapping Pass | ");
        } else {
            testPass = false;
            sBuilderResponse.append("Test ServletMapping Fail ; expecting [" + expectedResult + "] , found [" + result + "] | ");
        }

        LOG.info("Test attributes");

        /*
         * Test request attributes which should reflect the initial dispatcher servlet request information..not this servlet
         */

        sbFound.setLength(0); //reset
        result = new String();

        sbFound.append("ASYNC_REQUEST_URI=" + request.getAttribute("jakarta.servlet.async.request_uri"))
                        .append(", ASYNC_CONTEXT_PATH=" + request.getAttribute("jakarta.servlet.async.context_path"))
                        .append(", ASYNC_SERVLET_PATH=" + request.getAttribute("jakarta.servlet.async.servlet_path"))
                        .append(", ASYNC_PATH_INFO=" + request.getAttribute("jakarta.servlet.async.path_info"))
                        .append(", ASYNC_QUERY_STRING=" + request.getAttribute("jakarta.servlet.async.query_string"))
                        .append(", ASYNC_MAPPING=" + request.getAttribute("jakarta.servlet.async.mapping"));

        result = sbFound.toString();

        LOG.info(" found request attributes [" + result + "]");
        LOG.info(" expectedAttributesResult[" + expectedAttributesResult + "]");

        if (expectedAttributesResult.equals(result)) {
            sBuilderResponse.append("Test dispatch attributes Pass | ");
        } else {
            testPass = false;
            sBuilderResponse.append("Test dispatch attributes Fail ; expecting [" + expectedAttributesResult + "] , found [" + result + "] | ");
        }

        //Final result - any of the above tests fail will make test fail
        if (testPass)
            sBuilderResponse.append(" --]]] Result [PASS]");
        else
            sBuilderResponse.append(" --]]] Result [FAIL]");

        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
        LOG.info("Test ServletMapping dispatched Done.");
    }
}
