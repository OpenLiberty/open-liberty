/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.ApplicationRegistry;
import io.openliberty.microprofile.openapi20.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.OpenAPIUtils;
import io.smallrye.openapi.runtime.io.Format;

@WebServlet(name=Constants.SERVLET_NAME_APPLICATION, urlPatterns = {Constants.URL_PATTERN_ROOT}, loadOnStartup = 1)
public class ApplicationServlet extends OpenAPIServletBase {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(ApplicationServlet.class);
    
    private ServiceTracker<ApplicationRegistry, ApplicationRegistry> appRegistryTracker;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BundleContext bundleContext = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
        appRegistryTracker = new ServiceTracker<>(bundleContext, ApplicationRegistry.class, null);
        appRegistryTracker.open();
    }

    @Override
    public void destroy() {
        super.destroy();
        appRegistryTracker.close();
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Determine the format for the response and the correspoding content type
        final Format responseFormat = getResponseFormat(request);
        final String contentType = (responseFormat == Format.JSON) ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;

        // Make sure that the client is requesting /openapi only
        final String requestURI = request.getRequestURI();
        if (Constants.REGEX_COMPONENT_REQUEST_URI.matcher(requestURI).matches()) {

            /*
             * Retrieve the current provider and get the OpenAPI model for it. If there is no current provider then
             * generate a default (empty) OpenAPI model.
             */
            ApplicationRegistry appRegistry = appRegistryTracker.getService();
            OpenAPIProvider currentProvider = null;
            if (appRegistry != null) {
                currentProvider = appRegistry.getCurrentOpenAPIProvider();
            }
            final String document;
            if (currentProvider != null) {
                /*
                 * If the model that has been generated already contains server definitions, we trust that the user
                 * knows what they are doing and we do not modify the model in any way when the OpenAPI document is
                 * requested. If the model that has been been generated does not contain server definitions, we add them
                 * to the model before generating the OpenAPI document.  We need to synchronize access to the model
                 * while we are updating it.
                 */
                if (currentProvider.getServersDefined()) {
                    /*
                     * No need to modify the model. Retrieve the cached version of the OpenAPI document from the
                     * currentProvider in the specified format.
                     */
                    document = currentProvider.getOpenAPIDocument(responseFormat);
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Server information was already set by the user. So not setting Liberty's server information");
                    }
                } else {
                    /*
                     * The OpenAPI model that was generated does not contain any server definitions. We need to generate
                     * the server definitions, add them to the model and then generate the OpenAPI document in the
                     * specified format.
                     */
                    List<Server> servers = getOpenAPIModelServers(request, currentProvider.getApplicationPath());
                    document = currentProvider.getOpenAPIDocument(servers, responseFormat);
                }
            } else {
                /*
                 * No JAX-RS applications are currently running inside this OL instance. Create a default OpenAPI model,
                 * add some server definitions to it and then generate the OpenAPI document in the specified format.
                 */
                OpenAPI defaultOpenAPIModel = OpenAPIUtils.createBaseOpenAPIDocument();
                defaultOpenAPIModel.setServers(getOpenAPIModelServers(request));
                document = OpenAPIUtils.getOpenAPIDocument(defaultOpenAPIModel, responseFormat);
            }

            // Check to see if we have a valid OpenAPI document to return
            if (document != null) {
                writeResponse(response, document, Status.OK, contentType);
            } else {
                /*
                 * Something went wrong when attempting to serialize the OpenAPI model to a String in the
                 * specified format. Return 500 Internal Server Error.
                 */
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Null document. Return 500.");
                }
                writeResponse(response, null, Status.INTERNAL_SERVER_ERROR, contentType);
            }
        } else {
            /*
             * The client is attempting to retrieve a resource other than /openapi.
             * Return 404 Not Found.
             */
            writeResponse(response, null, Status.NOT_FOUND, contentType);
        }
    }
}
