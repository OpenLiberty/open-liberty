/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.servlet;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl;
import com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl;
import com.ibm.wsspi.rest.handler.RESTHandlerContainer;

/**
 * <p>Main portal for https://&lt;hostname&gt;:&lt;https_port&gt;/ibm/api/* requests. This servlet will delegate incoming request to the appropriate
 * {@link com.ibm.wsspi.rest.handler.RESTHandler}.
 * <br>If no rest handler is found for a given request, we return an error message.
 *
 * <p>A GET request to https://&lt;hostname&gt;:&lt;https_port&gt;/ibm/api will return a list of registered rest handler roots.
 */
public class RESTProxyServlet extends HttpServlet {
    private static final TraceComponent tc = Tr.register(RESTProxyServlet.class);
    private static final long serialVersionUID = 1L;

    private transient RESTHandlerContainer REST_HANDLER_CONTAINER = null;

    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        // Setup service - will handle
        getAndSetRESTHandlerContainer(request);

        // We have special logic for the context root
        if ("/".equals(request.getPathInfo())) {
            // We only handle GET requests on the context root
            //Note:  By design we're not checking for authorized roles here, because we're only exposing which URLs are available
            if ("GET".equals(request.getMethod())) {
                listRegisteredHandlers(response);
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            handleWithDelegate(request, response);
        }
    }

    /**
     * <p>Set into the response the container version and a list of registered roots.</p>
     *
     * <p>Our return JSON structure is:
     * 
     * <pre>
     * {
     * "version" : int,
     * "roots" : [ String* ]
     * }
     * </pre>
     * 
     * </p>
     *
     * @param response
     * @throws IOException If the JSON serialization fails
     */
    private void listRegisteredHandlers(final HttpServletResponse response) throws IOException {
        //Build the array of registered roots
        JSONArray keyArray = new JSONArray();
        Iterator<String> keys = REST_HANDLER_CONTAINER.registeredKeys();
        if (keys != null) {
            while (keys.hasNext()) {
                keyArray.add(keys.next());
            }
        }

        //Add the version.
        JSONObject jsonObject = new OrderedJSONObject();
        jsonObject.put("version", RESTHandlerContainer.REST_HANDLER_CONTAINER_VERSION);
        jsonObject.put("roots", keyArray);

        //The JSON library escapes forward slashes, but there's no need to in this case, so we unescape them.
        String serialized = jsonObject.serialize();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(serialized.replace("\\/", "/"));
    }

    /**
     * For any request URL other than the context root, delegate to the
     * appropriate handler. If no handler is available, a 404 will be set
     * into the response.
     *
     * @param request
     * @param response
     * @param pathInfo
     * @throws IOException
     */
    private void handleWithDelegate(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        //Delegate to handler
        boolean foundHandler = REST_HANDLER_CONTAINER.handleRequest(new ServletRESTRequestImpl(request), new ServletRESTResponseImpl(response));
        //RESTHandler handler = REST_HANDLER_CONTAINER.getHandler(pathInfo);
        if (!foundHandler) {
            //No handler found, so we send back a 404 "not found" response.
            String errorMsg = Tr.formatMessage(tc, "HANDLER_NOT_FOUND_ERROR", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
    }

    /**
     * Grabs the RESTHandlerContainer from the OSGi service registry and stores
     * it to {@link #REST_HANDLER_CONTAINER}.
     *
     * @param request The HttpServletRequest from which we'll get the OSGi BundleContext
     * @throws ServletException When the RESTHandlerContainer service is unavailable
     */
    private synchronized void getAndSetRESTHandlerContainer(HttpServletRequest request) throws ServletException {
        if (REST_HANDLER_CONTAINER == null) {
            //Get the bundle context
            HttpSession session = request.getSession();
            ServletContext sc = session.getServletContext();
            BundleContext ctxt = (BundleContext) sc.getAttribute("osgi-bundlecontext");

            ServiceReference<RESTHandlerContainer> ref = ctxt.getServiceReference(RESTHandlerContainer.class);
            if (ref == null) {
                // Couldn't find service, so throw the error.
                throw new ServletException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "RESTHandlerContainer"));
            } else {
                REST_HANDLER_CONTAINER = ctxt.getService(ref);
            }
        }
    }

}
