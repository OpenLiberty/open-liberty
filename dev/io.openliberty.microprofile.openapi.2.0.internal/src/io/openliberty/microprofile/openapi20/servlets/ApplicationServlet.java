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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.ApplicationProcessor;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.smallrye.openapi.runtime.io.Format;

@WebServlet(name=Constants.SERVLET_NAME_APPLICATION, urlPatterns = {Constants.URL_PATTERN_ROOT}, loadOnStartup = 1)
public class ApplicationServlet extends OpenAPIServletBase {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(ApplicationServlet.class);
    
    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Determine the format for the response and the correspoding content type
        final Format responseFormat = getResponseFormat(request);
        final String contentType = (responseFormat == Format.JSON) ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;

        // Make sure that the client is requesting /openapi only
        final String requestURI = request.getRequestURI();
        if (Constants.REGEX_COMPONENT_REQUEST_URI.matcher(requestURI).matches()) {

            // Make sure that we have a valid ApplicationProcessor
            ApplicationProcessor applicationProcessor = ApplicationProcessor.getInstance();
            if (applicationProcessor != null) {
                // Generate the Open API document in the requested format
                final String document = applicationProcessor.getOpenAPIDocument(request, responseFormat);
                
                // Build the response
                if (document != null) {
                    writeResponse(response, document, Status.OK, contentType);
                } else {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Null document (json). Return 500.");
                    }
                    writeResponse(response, null, Status.INTERNAL_SERVER_ERROR, contentType);
                }
            } else {
                writeResponse(response, "Failed to find OpenAPI application processor", Status.NOT_FOUND, MediaType.TEXT_PLAIN);
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
