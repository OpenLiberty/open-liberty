/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.Filter;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.MediaTypeNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Base RESTHandler class for handling JSON responses.</p>
 * 
 * <p>Supported HTTP methods:
 * <ul>
 * <li>GET - Read or retrieve the resource or collection</li>
 * <li>POST - Create or add a resource to a collection</li>
 * <li>PUT - Update a resource or collection</li>
 * <li>DELETE - Delete a resource or collection</li>
 * </ul>
 * </p>
 * <p>If a resource is requested of the handler that the handler is not expecting,
 * a 404 will be returned. The expected resources patterns are set in the handler's
 * constructor.</p>
 */
public abstract class CommonJSONRESTHandler extends CommonRESTHandler {
    private static final TraceComponent tc = Tr.register(CommonJSONRESTHandler.class);

    /**
     * Constructor which should be called by all extenders.
     * 
     * @param handlerURL The URL for which this handler is registered.
     *            Should not end with a trailing slash. Must not be {@code null}.
     * @param handlesChildResource Indicate whether or not child resources are
     *            expected to be handled by this handler. Note only immediate
     *            children are handled when this is set to true. Deeply nested
     *            children are not considered to match.
     * @param handlesGrandchildResource Indicate whether or not grandchild
     *            resources are expected to be handled by this handler. Note
     *            only immediate grandchildren are handled when this is set
     *            to true. Deeply nested grandchildren are not considered to
     *            match.
     */
    protected CommonJSONRESTHandler(final String handlerURL, final boolean handlesChildResource, final boolean handlesGrandchildResource) {
        super(handlerURL, handlesChildResource, handlesGrandchildResource, new Filter(), null);
    }

    /**
     * Unit test constructor.
     * 
     * @param handlerURL The URL for which this handler is registered.
     *            Should not end with a trailing slash. Must not be {@code null}.
     * @param handlesChildResource Indicate whether or not child resources are
     *            expected to be handled by this handler. Note only immediate
     *            children are handled when this is set to true. Deeply nested
     *            children are not considered to match.
     * @param handlesGrandchildResource Indicate whether or not grandchild
     *            resources are expected to be handled by this handler. Note
     *            only immediate grandchildren are handled when this is set
     *            to true. Deeply nested grandchildren are not considered to
     *            match.
     * @param filter Injection point for the Filter
     * @param mapper Injection point for the ObjectMapper
     */
    protected CommonJSONRESTHandler(final String handlerURL, final boolean handlesChildResource, final boolean handlesGrandchildResource, final Filter filter, final JSON json) {
        super(handlerURL, handlesChildResource, handlesGrandchildResource, filter, json);
    }

    /**
     * Validates the POST / PUT media type is JSON.
     * 
     * @param request The RESTRequest from handleRequest
     * @throws MediaTypeNotSupportedException if the Content-Type is not application/json
     */
    private void checkMediaTypeIsJson(final RESTRequest request) throws MediaTypeNotSupportedException {
        // Problem: Submitting {"name":"myTool","version":"1.0","url":"http://ibm.com","description":"IBM","icon":"default.png"} fails
        String requestContentType = request.getHeader(HTTP_HEADER_CONTENT_TYPE);
        if (requestContentType == null || requestContentType.indexOf(MEDIA_TYPE_APPLICATION_JSON_NO_CHARSET) < 0)
        {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Inbound Content-Type is not the required " +
                             MEDIA_TYPE_APPLICATION_JSON_NO_CHARSET + " rather it is " + requestContentType);
            }
            throw new MediaTypeNotSupportedException();
        }
    }

    /**
     * Delegates to the appropriate HTTP method handler, or sets the RESTResponse
     * to 405 if the method is not supported.
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @throws RESTException Re-throws any exceptions thrown by the delegates
     */
    @Override
    protected void delegateMethod(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String method = request.getMethod();
        if (HTTP_METHOD_GET.equals(method)) {
            setJSONResponse(response, doGET(request, response), HTTP_OK);
        } else if (HTTP_METHOD_POST.equals(method)) {
            checkMediaTypeIsJson(request);
            POSTResponse pr = doPOST(request, response);
            response.setResponseHeader("Location", pr.createdURL);
            setJSONResponse(response, pr.jsonPayload, HTTP_CREATED);
        } else if (HTTP_METHOD_PUT.equals(method)) {
            checkMediaTypeIsJson(request);
            setJSONResponse(response, doPUT(request, response), HTTP_OK);
        } else if (HTTP_METHOD_DELETE.equals(method)) {
            setJSONResponse(response, doDELETE(request, response), HTTP_OK);
        } else {
            throw new MethodNotSupportedException();
        }
    }

    /**
     * Applies the filter to the object. The filter is specified in the
     * RESTRequest 'field' parameter. If the filtering of the Object fails,
     * a RESTException indicating an internal error occurred. We intentionally
     * do not want to expose additional information to the client, but an FFDC
     * will be created for debugging. Note that we should not expect errors in
     * this path.
     * 
     * @param request The RESTRequest from handleRequest
     * @param obj The Object to filter
     * @return The filtered Object
     */

    protected final Object applyFilter(RESTRequest request, Object obj) throws RESTException {
        try {
            return filter.applyFieldFilter(request.getParameter("fields"), obj);
        } catch (Exception e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected Exception caught while applying filter to JSON Object", e);
            }
            throw new RESTException(HTTP_INTERNAL_ERROR);
        }
    }

    /**
     * Reads the JSON payload from the RESTRequest. If the JSON payload can not
     * be deserialized, then a BadRequestException is thrown.
     * 
     * @param request The RESTRequest from handleRequest
     * @param expectedType The Class to load from the JSON
     * @return The deserialized JSON object
     * @throws BadRequestException if the JSON payload was not in proper JSON
     *             syntax or if it did not match the expected JSON object
     */

    protected final <T> T readJSONPayload(RESTRequest request, Class<T> expectedType) throws RESTException {
        List<Class<? extends T>> types = new ArrayList<Class<? extends T>>();
        types.add(expectedType);
        return readJSONPayload(request, expectedType, types);
    }

    /**
     * Returns a comma delimited list of the Class names.
     * 
     * @param types List of Classes
     * @return The comma delimited list of class names.
     */
    @Trivial
    private final <T> String getSupportedTypes(List<Class<? extends T>> types) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> type : types) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(type.getCanonicalName());
        }
        return sb.toString();
    }

    /**
     * Reads the JSON payload from the RESTRequest. If the JSON payload can not
     * be deserialized, then a BadRequestException is thrown.
     * 
     * @param request The RESTRequest from handleRequest
     * @param superClass The parent Class of the types to load from the JSON. May be an interface.
     * @param concreteTypes The List of concrete types to load from the JSON. Must be concrete classes. Try order is based on the order in the list.
     * @return The deserialized JSON object
     * @throws BadRequestException if the JSON payload was not in proper JSON
     *             syntax or if it did not match the expected JSON object
     */
    @FFDCIgnore({ EOFException.class, JSONMarshallException.class })
    protected final <T> T readJSONPayload(RESTRequest request, Class<T> superClass, List<Class<? extends T>> concreteTypes) throws RESTException {
        try {
            String readerContents = getReaderContents(request.getInputStream(), POST_MAX_JSON_SIZE);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to parse RESTRequest POST payload as JSON", readerContents);
            }

            JSON jsonService = getJSONService();

            // Try to match JSON to one of the expected types
            for (Class<? extends T> type : concreteTypes) {
                try {
                    return jsonService.parse(readerContents, type);
                } catch (JSONMarshallException e) {
                    if (e.getMessage() != null && e.getMessage().equals("Unable to parse non-well-formed content")) {
                        throw e;
                    }
                    // Not the right match, move on
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Possibly expected JSONMarshallException while interpreting payload", e);
                    }
                }
            }

            // Nothing matched, the payload is of the wrong type
            Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "POST_WRONG_JSON_PAYLOAD", getSupportedTypes(concreteTypes)));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, error);
        } catch (EOFException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered EOFException while reading RESTRequest payload", e);
            }
            Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "POST_REQUIRES_JSON_PAYLOAD", getSupportedTypes(concreteTypes)));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, error);
        } catch (JSONMarshallException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered JSONMarshallException while reading RESTRequest payload", e);
            }
            Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "POST_BAD_JSON_PAYLOAD", getSupportedTypes(concreteTypes)));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, error);
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected IOException while reading RESTRequest payload", e);
            }
        }

        // If we don't have a reader, or we get an IOException, generic 500
        // response since there is nothing the caller can do to help here
        throw new RESTException(HTTP_INTERNAL_ERROR);
    }

    /**
     * Processes the RESTException thrown by delegateMethod().
     * 
     * @param response The RESTResponse from handleRequest
     * @param e The RESTException thrown by the delegate doX method
     * @throws IOException
     */
    @Override
    protected final void handleRESTException(final RESTResponse response, RESTException e) throws IOException {
        if (e.getPayload() != null) {
            String contentType = e.getContentType();
            if (MEDIA_TYPE_TEXT_PLAIN.equals(contentType)) {
                response.setStatus(e.getStatus());
                response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, e.getContentType());
                response.getOutputStream().write(e.getPayload().toString().getBytes(Charset.forName("UTF-8")));
            } else if (contentType != null && contentType.indexOf(MEDIA_TYPE_APPLICATION_JSON_NO_CHARSET) > -1) {
                setJSONResponse(response, e.getPayload(), e.getStatus());
            } else {
                response.setStatus(HTTP_INTERNAL_ERROR);
                response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_TEXT_PLAIN);
                response.getOutputStream().write(("An internal error occurred. RESTException had a set payload but did not specify content type").getBytes(Charset.forName("UTF-8")));
            }
        } else {
            response.setStatus(e.getStatus());
        }
    }

    @Trivial
    @Override
    protected final String getReaderContents(InputStream input, int maxSize) throws IOException, BadRequestException {
        try
        {
            return super.getReaderContents(input, maxSize);
        } catch (BadRequestException e)
        {
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, e.getPayload());
        }
    }

}
