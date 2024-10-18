/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi20.internal.services.ApplicationRegistry;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIInfoConfig;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIUtils;
import io.smallrye.openapi.runtime.io.Format;

@WebServlet(name = Constants.SERVLET_NAME_APPLICATION, urlPatterns = { Constants.URL_PATTERN_ROOT }, loadOnStartup = 1)
public class ApplicationServlet extends OpenAPIServletBase {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(ApplicationServlet.class);

    private ServiceTracker<ApplicationRegistry, ApplicationRegistry> appRegistryTracker;
    private ServiceTracker<OpenAPIModelOperations, OpenAPIModelOperations> modelOperationsTracker;
    private ServiceTracker<OpenAPIVersionConfig, OpenAPIVersionConfig> versionConfigTracker;
    private ServiceTracker<OpenAPIInfoConfig, OpenAPIInfoConfig> infoConfigTracker;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BundleContext bundleContext = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
        appRegistryTracker = new ServiceTracker<>(bundleContext, ApplicationRegistry.class, null);
        appRegistryTracker.open();
        modelOperationsTracker = new ServiceTracker<>(bundleContext, OpenAPIModelOperations.class, null);
        modelOperationsTracker.open();
        versionConfigTracker = new ServiceTracker<>(bundleContext, OpenAPIVersionConfig.class, null);
        versionConfigTracker.open();
        infoConfigTracker = new ServiceTracker<>(bundleContext, OpenAPIInfoConfig.class, null);
        infoConfigTracker.open();
    }

    @Override
    public void destroy() {
        super.destroy();
        appRegistryTracker.close();
        modelOperationsTracker.close();
        versionConfigTracker.close();
        infoConfigTracker.close();
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Determine the format for the response and the correspoding content type
        final Format responseFormat = getResponseFormat(request);
        final String contentType = (responseFormat == Format.JSON) ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;

        // Make sure that the client is requesting /openapi only
        final String servletPath = request.getServletPath();
        if (servletPath == null || servletPath.equals("/")) {

            /*
             * Retrieve the current provider and get the OpenAPI model for it. If there is no current provider then
             * generate a default (empty) OpenAPI model.
             */
            ApplicationRegistry appRegistry = appRegistryTracker.getService();
            OpenAPIProvider currentProvider = null;
            if (appRegistry != null) {
                currentProvider = appRegistry.getOpenAPIProvider();
            }
            final String document;
            if (currentProvider != null) {

                // Take a shallow copy of the model so we can change the servers and info
                OpenAPI model = modelOperationsTracker.getService().shallowCopy(currentProvider.getModel());

                if (OpenAPIUtils.containsServersDefinition(currentProvider.getModel())) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Server information was already set by the user. So not setting Liberty's server information");
                    }
                } else {
                    /*
                     * The OpenAPI model that was generated does not contain any server definitions. We need to generate
                     * the server definitions, and add them to the model
                     */
                    List<Server> servers = getOpenAPIModelServers(request, currentProvider.getApplicationPath());
                    model.setServers(servers);
                }

                Info configuredInfo;
                if (ProductInfo.getBetaEdition()) {
                    configuredInfo = infoConfigTracker.getService().getInfo()
                                                      .orElseGet(() -> OpenAPIUtils.getConfiguredInfo(ConfigProvider.getConfig()));
                } else {
                    configuredInfo = OpenAPIUtils.getConfiguredInfo(ConfigProvider.getConfig());
                }

                if (configuredInfo != null) {
                    model.setInfo(configuredInfo);
                }

                if (ProductInfo.getBetaEdition()) {
                    versionConfigTracker.getService().applyConfig(model);
                }

                document = OpenAPIUtils.getOpenAPIDocument(model, responseFormat);
            } else {
                /*
                 * No JAX-RS applications are currently running inside this OL instance. Create a default OpenAPI model,
                 * add some server definitions to it and then generate the OpenAPI document in the specified format.
                 */
                OpenAPI defaultOpenAPIModel = modelOperationsTracker.getService().createDefaultOpenApiModel();
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
