/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.PlatformProcessor;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

@WebServlet(
    name=Constants.SERVLET_NAME_PLATFORM,
    urlPatterns = {Constants.URL_PATTERN_PLATFORM, Constants.URL_PATTERN_PLATFORM_WILDCARD},
    loadOnStartup = 1
)
public class PlatformServlet extends OpenAPIServletBase {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(PlatformServlet.class);
    
    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Determine the format for the response and the correspoding content type
        final Format responseFormat = getResponseFormat(request);
        final String contentType = (responseFormat == Format.JSON) ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;
        
        // Check to see what is being requqsted... the components list or the OpenAPI doc for a specific component
        final String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            
            // Retrieve the components document
            final String document = PlatformProcessor.getComponentsDocument(getResponseFormat(request));
            
            // Build the response
            if (document != null) {
                writeResponse(response, document, Status.OK, contentType);
            } else {
                // Should never happen, but handle it just in case
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Null document (json). Return 500.");
                }
                writeResponse(response, null, Status.INTERNAL_SERVER_ERROR, contentType);
            }
        } else if (pathInfo != null && Constants.REGEX_COMPONENT_PATH_INFO.matcher(pathInfo).matches()) {
            
            // Extract the component name and attempt to retrieve the OpenAPI document for that component
            final String componentName = pathInfo.replace("/", "");
            OpenAPI openAPIModel = PlatformProcessor.getComponentOpenAPIModel(componentName);
            if (openAPIModel != null) {
                
                // Add servers to the model if they are not already present
                processServers(request, openAPIModel);
                
                try {
                    // Serialize the OpenAPI model to a String in the specified format and write the response
                    final String document = OpenApiSerializer.serialize(openAPIModel, responseFormat);
                    writeResponse(response, document, Status.OK, contentType);
                } catch (IOException e) {
                    /*
                     * Something went wrong when attempting to serialize the OpenAPI model to a String in the specified
                     * format.
                     * Return 500 Internal Server Error.
                     */
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Failed to serialize OpenAPI docuemnt: " + e.getMessage());
                    }
                    writeResponse(response, null, Status.INTERNAL_SERVER_ERROR, contentType);
                }
            } else {
                /*
                 * The specified component has not registered to expose an OpenAPI document that describes its APIs.
                 * Return 404 Not Found.
                 */
                writeResponse(response, null, Status.NOT_FOUND, contentType);
            }
        } else {
            /*
             * The client is attempting to retrieve a resource other than /openapi/platform or
             * /openapi/platform/<COMPONENT>.
             * Return 404 Not Found.
             */
            writeResponse(response, null, Status.NOT_FOUND, contentType);
        }
    }
}
