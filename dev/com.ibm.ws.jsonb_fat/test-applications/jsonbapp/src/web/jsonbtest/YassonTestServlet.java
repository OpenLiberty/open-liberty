/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package web.jsonbtest;

import static web.jsonbtest.JSONBTestServlet.PROVIDER_JOHNZON;
import static web.jsonbtest.JSONBTestServlet.PROVIDER_YASSON;

import java.util.HashMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/YassonTestServlet")
public class YassonTestServlet extends FATServlet {

    @Test
    public void testApplicationClasses(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomHttpRequest newRequest = new CustomHttpRequest(request);
        newRequest.addParameter("JsonbProvider", PROVIDER_YASSON);
        JSONBTestServlet.testApplicationClasses(newRequest, response);
    }

    @Test
    public void testJsonbAdapter(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomHttpRequest newRequest = new CustomHttpRequest(request);
        newRequest.addParameter("JsonbProvider", PROVIDER_YASSON);
        JSONBTestServlet.testJsonbAdapter(newRequest, response);
    }

    @Test
    public void testJsonbProviderAvailable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomHttpRequest newRequest = new CustomHttpRequest(request);
        newRequest.addParameter("JsonbProvider", PROVIDER_YASSON);
        JSONBTestServlet.testJsonbProviderAvailable(newRequest, response);
    }

    @Test
    public void testJsonbProviderNotAvailable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomHttpRequest newRequest = new CustomHttpRequest(request);
        newRequest.addParameter("JsonbProvider", PROVIDER_JOHNZON);
        JSONBTestServlet.testJsonbProviderNotAvailable(newRequest, response);
    }

    @Test
    public void testThreadContextClassLoader(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CustomHttpRequest newRequest = new CustomHttpRequest(request);
        newRequest.addParameter("JsonbProvider", PROVIDER_YASSON);
        JSONBTestServlet.testThreadContextClassLoader(newRequest, response);
    }

    public class CustomHttpRequest extends HttpServletRequestWrapper {

        private HashMap<String, String> params = new HashMap<>();

        public CustomHttpRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            // if we added one, return that one
            if (params.get(name) != null) {
                return params.get(name);
            }

            // otherwise return what's in the original request
            HttpServletRequest req = (HttpServletRequest) super.getRequest();
            return req.getParameter(name);
        }

        public void addParameter(String name, String value) {
            params.put(name, value);
        }

    }
}
