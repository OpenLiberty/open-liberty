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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.openapi20.DefaultHostListener;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIUtils;
import io.openliberty.microprofile.openapi20.utils.ProxySupportUtil;
import io.openliberty.microprofile.openapi20.utils.ServerInfo;
import io.smallrye.openapi.runtime.io.Format;

public abstract class OpenAPIServletBase extends HttpServlet {

    private static final long serialVersionUID = -6021365340147075272L;

    /**
     * The getResponseFormat method determines the format of the document that should be sent in the body of the
     * response. The client can specify the desired format using either the Accept header or by specifying a format
     * query parameter. The format defaults to YAML if no format is specified by the client. 
     * 
     * @param request
     *          The HttpServletRequest object.
     * @return Format
     *          The format of the response document. It should be either Format.YAML or Format.JSON. 
     */
    protected Format getResponseFormat(HttpServletRequest request) {
        // Create the variable to return... defaulting the response format to YAML
        Format format = Format.YAML;

        // Check the list of acceptable media types in the Accpet header for a JSON compatible type 
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptHeader != null) {
            for (String value : acceptHeader.split(",")) {
                if (  value.trim().equals(MediaType.APPLICATION_JSON)
                   || value.trim().equals(Constants.MEDIA_TYPE_APPLICATION_JS)
                   || value.trim().equals(Constants.MEDIA_TYPE_TEXT_JS)
                   || value.trim().equals(Constants.MEDIA_TYPE_TEXT_JSON)
                   ) {
                    format = Format.JSON;
                    break;
                }
            } // FOR
        }

        // Check the format parameter
        String formatParam = request.getParameter(Constants.FORMAT_PARAM_NAME);
        if (formatParam != null && formatParam.equalsIgnoreCase(Constants.FORMAT_PARAM_VALUE_JSON)) {
            format = Format.JSON;
        }

        return format;
    }
    
    /**
     * The getDefaultHostServerInfo method returns the ServerInfo object for the default_host virtual host.
     * 
     * @return ServerInfo
     *          The server info for the default_host virtual host
     */
    protected ServerInfo getDefaultHostServerInfo() throws ServletException {
        return DefaultHostListener.getInstance().getDefaultHostServerInfo();
    }
    
    /**
     * The getOpenAPIModelServers method generates a list of servers based on the information available in:
     * 
     *  - The ServerInfo for the default_host virtual host
     *  - The HTTP request url
     *  - The HTTP Referer header
     * 
     * @param request
     *          The HTTPServletRequest
     * @return List<Server>
     *          The list of OpenAPI model servers
     * @throws ServletException
     */
    protected List<Server> getOpenAPIModelServers(final HttpServletRequest request) throws ServletException {
        return getOpenAPIModelServers(request, null);
    }

    /**
     * The getOpenAPIModelServers method generates a list of servers based on the information available in:
     * 
     *  - The ServerInfo for the default_host virtual host
     *  - The HTTP request url
     *  - The HTTP Referer header
     *  - The context root for the web module that was used to generate the OpenAPI model
     * 
     * @param request
     *          The HTTPServletRequest
     * @param applciationPath
     *          The application path for the application that was used to generate the OpenAPI model
     * @return List<Server>
     *          The list of OpenAPI model servers
     * @throws ServletException
     */
    protected List<Server> getOpenAPIModelServers(final HttpServletRequest request, final String applciationPath) throws ServletException {
        ServerInfo serverInfo = new ServerInfo(getDefaultHostServerInfo());
        ProxySupportUtil.processRequest(request, serverInfo);
        return OpenAPIUtils.getOpenAPIModelServers(serverInfo, applciationPath);
    }

    /**
     * The writeResponse method generates the HTTP response based on the parameters passed in to the method.
     * 
     * @param response
     *          The HTTPServletResponse object
     * @param document
     *          The document to write to the reponse body, or null if no response body is required
     * @param status
     *          The HTTP status code to send in the response
     * @param contentType
     *          The content type for the response
     * @throws IOException
     */
    @Trivial
    protected void writeResponse(
        final HttpServletResponse response,
        final String document,
        final Status status,
        final String contentType
    ) throws IOException {
        response.setStatus(status.getStatusCode());
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (document != null) {
            Writer writer = response.getWriter();
            writer.write(document);
        }
    }
}
