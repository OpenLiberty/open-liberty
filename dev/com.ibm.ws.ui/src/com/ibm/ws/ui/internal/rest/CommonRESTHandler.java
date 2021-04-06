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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONFactory;
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
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>RESTHandler class for handling tool data responses.</p>
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
public class CommonRESTHandler implements AdminCenterRestHandler, APIConstants, HTTPConstants {
    private static final TraceComponent tc = Tr.register(CommonRESTHandler.class);

    protected final Filter filter;
    protected final String handlerURL;
    protected final boolean handlesChildResource;
    protected final boolean handlesGrandchildResource;
    protected static final String KEY_JSON_SERVICE = "jsonService";
    protected static final String ADMINISTRATOR_ROLE_NAME = "Administrator";
    protected static final String ADMIN_RESOURCE_NAME = "com.ibm.ws.management.security.resource";
    protected static final String ALL_AUTHENTICATED_USERS_ROLE_NAME = "allAuthenticatedUsers";
    protected static final String READER_ROLE_NAME = "Reader";
    /**
     * The set of roles that is sufficient by default to do most HTTP operations.
     */
    protected static final Set<String> REQUIRED_ROLES_DEFAULT = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { ADMINISTRATOR_ROLE_NAME })));

    /**
     * The set of roles that is sufficient to perform a GET operation or any operation on /ibm/api/adminCenter/v1/toolbox.
     */
    protected static final Set<String> REQUIRED_ROLES_GET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { ADMINISTRATOR_ROLE_NAME,
                                                                                                                                         READER_ROLE_NAME })));
    private JSON json;

    /**
     * Constructor which should be called by all extenders.
     *
     * @param handlerURL                The URL for which this handler is registered.
     *                                      Should not end with a trailing slash. Must not be {@code null}.
     * @param handlesChildResource      Indicate whether or not child resources are
     *                                      expected to be handled by this handler. Note only immediate
     *                                      children are handled when this is set to true. Deeply nested
     *                                      children are not considered to match.
     * @param handlesGrandchildResource Indicate whether or not grandchild
     *                                      resources are expected to be handled by this handler. Note
     *                                      only immediate grandchildren are handled when this is set
     *                                      to true. Deeply nested grandchildren are not considered to
     *                                      match.
     */
    protected CommonRESTHandler(final String handlerURL, final boolean handlesChildResource, final boolean handlesGrandchildResource) {
        this(handlerURL, handlesChildResource, handlesGrandchildResource, new Filter(), null);
    }

    /**
     * Unit test constructor.
     *
     * @param handlerURL                The URL for which this handler is registered.
     *                                      Should not end with a trailing slash. Must not be {@code null}.
     * @param handlesChildResource      Indicate whether or not child resources are
     *                                      expected to be handled by this handler. Note only immediate
     *                                      children are handled when this is set to true. Deeply nested
     *                                      children are not considered to match.
     * @param handlesGrandchildResource Indicate whether or not grandchild
     *                                      resources are expected to be handled by this handler. Note
     *                                      only immediate grandchildren are handled when this is set
     *                                      to true. Deeply nested grandchildren are not considered to
     *                                      match.
     * @param filter                    Injection point for the Filter
     * @param mapper                    Injection point for the ObjectMapper
     */
    protected CommonRESTHandler(final String handlerURL, final boolean handlesChildResource, final boolean handlesGrandchildResource, final Filter filter,
                                final JSON json) {
        this.handlerURL = handlerURL;
        this.handlesChildResource = handlesChildResource;
        this.handlesGrandchildResource = handlesGrandchildResource;
        this.filter = filter;
        this.json = json;
    }

    /**
     * Utility that returns a JSON object from a factory
     *
     * @return the JSON object providing POJO-JSON serialization and deserialization
     * @throws IOException if there are problems obtaining the OSGi service
     */
    protected JSON getJSONService() throws JSONMarshallException {
        if (json != null) {
            return json;
        }
        json = JSONFactory.newInstance();
        return json;
    }

    /** {@inheritDoc} */
    @Override
    public String baseURL() {
        return handlerURL;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChildren() {
        return handlesChildResource;
    }

    @Override
    public boolean hasGrandchildren() {
        return handlesGrandchildResource;
    }

    /**
     * Determines the child resource from the requested resource path based on
     * the handlerURL this class was initialized with.
     *
     * @param requestedResource The requested resource.
     * @return The child resource name (with no slashes) if the requested resource
     *         was a child resource. If the requested resource was not a child
     *         resource, or if the requested resource contained more than 1 child, {@code null} is returned.
     */
    final String getChildResourceName(String requestedResource) {
        if (requestedResource == null) {
            return null;
        }

        // Strip trailing slash if present
        if (requestedResource.endsWith("/")) {
            requestedResource = requestedResource.substring(0, requestedResource.length() - 1);
        }

        if (!requestedResource.startsWith(handlerURL)) {
            return null;
        }

        String childResource = requestedResource.substring(handlerURL.length());

        // Its either the base resource or the base resource with a trailing slash
        if (childResource.length() < 1) {
            return null;
        } else {
            childResource = childResource.substring(1);
            // And child resource has more than just the leading slash...
            if (childResource.contains("/")) {
                int nextSlash = childResource.indexOf('/');
                return childResource.substring(0, nextSlash);
            } else {
                return childResource;
            }
        }
    }

    /**
     * Determines the grandchild resource from the requested resource path based on
     * the handlerURL this class was initialized with.
     *
     * @param requestedResource The requested resource.
     * @return The child resource name (with no slashes) if the requested resource
     *         was a child resource. If the requested resource was not a child
     *         resource, or if the requested resource contained more than 1 child, {@code null} is returned.
     */
    final String getGrandchildResourceName(String requestedResource) {
        if (requestedResource == null) {
            return null;
        }

        // Strip trailing slash if present
        if (requestedResource.endsWith("/")) {
            requestedResource = requestedResource.substring(0, requestedResource.length() - 1);
        }

        if (!requestedResource.startsWith(handlerURL)) {
            return null;
        }

        String childResource = requestedResource.substring(handlerURL.length());

        // Its either the base resource or the base resource with a trailing slash
        if (childResource.length() < 1) {
            return null;
        } else {
            childResource = childResource.substring(1);
            // And child resource has more than just the leading slash...
            if (childResource.contains("/")) {
                int nextSlash = childResource.indexOf('/');
                String grandchild = childResource.substring(nextSlash + 1);
                if (grandchild.contains("/")) {
                    nextSlash = grandchild.indexOf('/');
                    return grandchild.substring(0, nextSlash);
                } else {
                    return grandchild;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Determines if the requested resource is the base resource for this
     * handler URL.
     *
     * @param requestedResource The requested resource.
     * @return {@code true} if the requested resource is the base URL to be handled by this handler; {@code false} otherwise.
     */
    final boolean isBaseResource(String requestedResource) {
        if (requestedResource == null) {
            return false;
        }

        // Strip trailing slash if present
        if (requestedResource.endsWith("/")) {
            requestedResource = requestedResource.substring(0, requestedResource.length() - 1);
        }

        return handlerURL.equals(requestedResource);
    }

    /**
     * Determines if the requested resource this a direct child resource for
     * the handler.
     *
     * @param requestedResource The requested resource.
     * @return {@code true} if the requested resource is direct child of this handler; {@code false} otherwise.
     */
    final boolean isChildResource(String requestedResource) {
        return getChildResourceName(requestedResource) != null && getGrandchildResourceName(requestedResource) == null;
    }

    /**
     * Determines if the requested resource this a direct grandchild resource
     * for the handler.
     *
     * @param requestedResource The requested resource.
     * @return {@code true} if the requested resource is direct grandchild of this handler; {@code false} otherwise.
     */
    final boolean isGrandchildResource(String requestedResource) {
        // Yeah copy paste is lame...
        if (requestedResource == null) {
            return false;
        }

        // Strip trailing slash if present
        if (requestedResource.endsWith("/")) {
            requestedResource = requestedResource.substring(0, requestedResource.length() - 1);
        }

        if (!requestedResource.startsWith(handlerURL)) {
            return false;
        }

        String childResource = requestedResource.substring(handlerURL.length());

        // Its either the base resource or the base resource with a trailing slash
        if (childResource.length() < 1) {
            return false;
        } else {
            childResource = childResource.substring(1);
            // And child resource has more than just the leading slash...
            if (childResource.contains("/")) {
                int nextSlash = childResource.indexOf('/');
                String grandchild = childResource.substring(nextSlash + 1);
                if (grandchild.contains("/")) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * When you have children, this method must be implemented.
     *
     * @param child
     * @return {@code true} if the child is recognized, {@code false} otherwise.
     */
    @Override
    public boolean isKnownChildResource(final String child, final RESTRequest request) {
        if (hasChildren()) {
            throw new IllegalStateException("When you have children, this method needs to be implemented");
        } else {
            return false;
        }
    }

    /**
     * When you have grandchildren, this method must be implemented.
     *
     * @param child
     * @param grandchild
     * @return {@code true} if the child is recognized, {@code false} otherwise.
     */
    @Override
    public boolean isKnownGrandchildResource(final String child, final String grandchild, final RESTRequest request) {
        if (hasGrandchildren()) {
            throw new IllegalStateException("When you have grandchildren, this method needs to be implemented");
        } else {
            return false;
        }
    }

    /**
     * <p>Handles the GET method on the base resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Object getBase(final RESTRequest request, final RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    /**
     * <p>Handles the GET method on a child of the resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Object getChild(final RESTRequest request, final RESTResponse response, final String child) throws RESTException {
        if (isKnownChildResource(child, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the GET method on a grandchild of the resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Object getGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (isKnownGrandchildResource(child, grandchild, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the GET method.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    protected final Object doGET(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String requestPath = request.getPath();
        if (isBaseResource(requestPath)) {
            return getBase(request, response);
        } else if (isChildResource(requestPath)) {
            return getChild(request, response, getChildResourceName(requestPath));
        } else if (isGrandchildResource(requestPath)) {
            return getGrandchild(request, response, getChildResourceName(requestPath), getGrandchildResourceName(requestPath));
        } else {
            throw new NoSuchResourceException();
        }
    }

    @Override
    public POSTResponse postBase(final RESTRequest request, final RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    @Override
    public POSTResponse postChild(final RESTRequest request, final RESTResponse response, final String child) throws RESTException {
        if (isKnownChildResource(child, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the POST method on a grandchild of the resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public POSTResponse postGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (isKnownGrandchildResource(child, grandchild, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the POST method.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    protected final POSTResponse doPOST(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String requestPath = request.getPath();
        if (isBaseResource(requestPath)) {
            return postBase(request, response);
        } else if (isChildResource(requestPath)) {
            return postChild(request, response, getChildResourceName(requestPath));
        } else if (isGrandchildResource(requestPath)) {
            return postGrandchild(request, response, getChildResourceName(requestPath), getGrandchildResourceName(requestPath));
        } else {
            throw new NoSuchResourceException();
        }
    }

    @Override
    public Object putBase(final RESTRequest request, final RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    @Override
    public Object putChild(final RESTRequest request, final RESTResponse response, final String child) throws RESTException {
        if (isKnownChildResource(child, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the PUT method on a grandchild of the resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Object putGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (isKnownGrandchildResource(child, grandchild, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the PUT method.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    protected final Object doPUT(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String requestPath = request.getPath();
        if (isBaseResource(requestPath)) {
            return putBase(request, response);
        } else if (isChildResource(requestPath)) {
            return putChild(request, response, getChildResourceName(requestPath));
        } else if (isGrandchildResource(requestPath)) {
            return putGrandchild(request, response, getChildResourceName(requestPath), getGrandchildResourceName(requestPath));
        } else {
            throw new NoSuchResourceException();
        }
    }

    @Override
    public Object deleteBase(final RESTRequest request, final RESTResponse response) throws RESTException {
        throw new MethodNotSupportedException();
    }

    @Override
    public Object deleteChild(final RESTRequest request, final RESTResponse response, final String child) throws RESTException {
        if (isKnownChildResource(child, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the DELETE method on a grandchild of the resource.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    @Override
    public Object deleteGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (isKnownGrandchildResource(child, grandchild, request)) {
            throw new MethodNotSupportedException();
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * <p>Handles the DELETE method.</p>
     *
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     */
    protected final Object doDELETE(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String requestPath = request.getPath();
        if (isBaseResource(requestPath)) {
            return deleteBase(request, response);
        } else if (isChildResource(requestPath)) {
            return deleteChild(request, response, getChildResourceName(requestPath));
        } else if (isGrandchildResource(requestPath)) {
            return deleteGrandchild(request, response, getChildResourceName(requestPath), getGrandchildResourceName(requestPath));
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Validates the POST / PUT media type is plain text.
     *
     * @param request The RESTRequest from handleRequest
     * @throws MediaTypeNotSupportedException if the Content-Type is not text/plain
     */
    private void checkMediaTypeIsPlainText(final RESTRequest request) throws MediaTypeNotSupportedException {
        // Problem: Submitting {"name":"myTool","version":"1.0","url":"http://ibm.com","description":"IBM","icon":"default.png"} fails
        String requestContentType = request.getHeader(HTTP_HEADER_CONTENT_TYPE);
        if (requestContentType == null || requestContentType.indexOf(MEDIA_TYPE_TEXT_PLAIN) < 0) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Inbound Content-Type is not the required " +
                             MEDIA_TYPE_TEXT_PLAIN + " rather it is " + requestContentType);
            }
            throw new MediaTypeNotSupportedException();
        }
    }

    /**
     * Delegates to the appropriate HTTP method handler, or sets the RESTResponse
     * to 405 if the method is not supported.
     *
     * @param request  The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @throws RESTException Re-throws any exceptions thrown by the delegates
     */
    protected void delegateMethod(final RESTRequest request, final RESTResponse response) throws RESTException {
        final String method = request.getMethod();
        if (HTTP_METHOD_GET.equals(method)) {
            setPlainTextResponse(response, doGET(request, response), HTTP_OK);
        } else if (HTTP_METHOD_POST.equals(method)) {
            checkMediaTypeIsPlainText(request);
            POSTResponse pr = doPOST(request, response);
            response.setResponseHeader("Location", pr.createdURL);
            setPlainTextResponse(response, pr.jsonPayload, HTTP_CREATED);
        } else if (HTTP_METHOD_PUT.equals(method)) {
            checkMediaTypeIsPlainText(request);
            Object obj = doPUT(request, response);
            if (obj instanceof Message)
                setJSONResponse(response, obj, HTTP_OK);
            else
                setPlainTextResponse(response, obj, HTTP_OK);
        } else if (HTTP_METHOD_DELETE.equals(method)) {
            Object obj = doDELETE(request, response);
            if (obj instanceof Message)
                setJSONResponse(response, obj, HTTP_OK);
            else
                setPlainTextResponse(response, obj, HTTP_OK);
        } else {
            throw new MethodNotSupportedException();
        }
    }

    /**
     * <p>Sets the RESTResponse payload with the specific POJO's JSON String.
     * Also sets the RESTResponse content type as JSON and status code of 200.</p>
     * <p>If the specified POJO can not be converted into a JSON String, then
     * return a 500 to indicate something went wrong. We can't do much more,
     * but we shouldn't need to do that since we should always POJO'able.</p>
     *
     * @param response The RESTResponse from handleRequest
     * @param pojo     The POJO to convert to a JSON object and set in the response payload
     * @param status   The desired HTTPS status to set
     */
    protected final void setPlainTextResponse(final RESTResponse response, final Object obj, final int status) {
        response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_TEXT_PLAIN);

        try {
            // Deserialize first. We need to know it will work before we set status
            byte[] b = obj.toString().getBytes("UTF-8");
            response.setStatus(status);
            response.getOutputStream().write(b);
        } catch (IOException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected IOException while writing out POJO response", e);
            }
            response.setStatus(HTTP_INTERNAL_ERROR);
        }
    }

    /**
     * <p>Sets the RESTResponse payload with the specific POJO's JSON String.
     * Also sets the RESTResponse content type as JSON and status code of 200.</p>
     * <p>If the specified POJO can not be converted into a JSON String, then
     * return a 500 to indicate something went wrong. We can't do much more,
     * but we shouldn't need to do that since we should always POJO'able.</p>
     *
     * @param response The RESTResponse from handleRequest
     * @param pojo     The POJO to convert to a JSON object and set in the response payload
     * @param status   The desired HTTPS status to set
     */
    protected final void setJSONResponse(final RESTResponse response, final Object pojo, final int status) {
        response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);

        try {
            // Deserialize first. We need to know it will work before we set status
            JSON jsonService = getJSONService();
            byte[] b = jsonService.asBytes(pojo);
            response.setStatus(status);
            response.getOutputStream().write(b);
        } catch (JSONMarshallException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected JSONMarshallException while writing out POJO response", e);
            }
            response.setStatus(HTTP_INTERNAL_ERROR);
        } catch (IOException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected IOException while writing out POJO response", e);
            }
            response.setStatus(HTTP_INTERNAL_ERROR);
        }
    }

    /**
     * Determines if the requested resource this handler was invoked for is
     * actually a resource we are willing to handle.
     *
     * @param requestedResource The requested resource.
     * @return {@code true} if the requested resource is expected to be handled by this handler; {@code false} otherwise.
     */
    protected final boolean matchesExpectedResource(String requestedResource) {
        if (requestedResource == null) {
            return false;
        }

        // Strip trailing slash if present
        if (requestedResource.endsWith("/")) {
            requestedResource = requestedResource.substring(0, requestedResource.length() - 1);
        }

        // Direct match (trailing slash has been removed)
        if (handlerURL.equals(requestedResource)) {
            return true;
        }

        // If we handle children...
        if (handlesChildResource) {
            String childResource = requestedResource.substring(handlerURL.length());

            // And child resource has more than just the leading slash...
            if (childResource.substring(1).contains("/")) {
                if (handlesGrandchildResource) {
                    int nextSlash = childResource.substring(1).indexOf('/');
                    String grandchild = childResource.substring(nextSlash + 2); // 2 for the first and second slash
                    if (grandchild.contains("/")) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes the RESTException thrown by delegateMethod().
     *
     * @param response The RESTResponse from handleRequest
     * @param e        The RESTException thrown by the delegate doX method
     * @throws IOException
     */
    protected void handleRESTException(final RESTResponse response, RESTException e) throws IOException {
        if (e.getPayload() != null) {
            String contentType = e.getContentType();
            if (MEDIA_TYPE_TEXT_PLAIN.equals(contentType)) {
                response.setStatus(e.getStatus());
                response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, e.getContentType());
                response.getOutputStream().write(e.getPayload().toString().getBytes(Charset.forName("UTF-8")));
            } else {
                response.setStatus(HTTP_INTERNAL_ERROR);
                response.setResponseHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_TEXT_PLAIN);
                response.getOutputStream().write(("An internal error occurred. RESTException had a set payload but did not specify content type").getBytes(Charset.forName("UTF-8")));
            }
        } else {
            response.setStatus(e.getStatus());
        }
    }

    /**
     * {@inheritDoc} <p>This method defines the default behaviour for all handlers, both
     * those which only handle themselves, and those which handle direct children.
     * <b>This method should not be overriden by extenders.</b></p>
     * <p>This method will invoke delegate methods for GET, POST, PUT and
     * DELETE, and it will also protect against the handler being called for
     * a resource which this is not handling, such as a deeply nested child
     * resource.</p>
     * <p>This method also guards against any RuntimeExceptions that may be
     * encountred while processing. These internal errors must not be propgated
     * back to the caller.</p>
     */
    @Override
    @FFDCIgnore(RESTException.class)
    public final void handleRequest(final RESTRequest request, final RESTResponse response) throws IOException {
        try {
            if (matchesExpectedResource(request.getPath())) {
                try {
                    delegateMethod(request, response);
                } catch (RESTException e) {
                    handleRESTException(response, e);
                }
            } else {
                response.setStatus(HTTP_NOT_FOUND);
            }
        } catch (RuntimeException e) {
            // Trap, log and suppress - we do not want to return internal details to the client
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected RuntimeException caught during handleRequest", e);
            }
            response.setStatus(HTTP_INTERNAL_ERROR);
        }
    }

    /**
     * Gets a String representing the Reader's input stream. The maximum size
     * of the content is limited by {@link APIConstants#POST_MAX_JSON_SIZE}.
     *
     * @param input The input Reader
     * @return The String representing the Reader's contents
     * @throws IOException
     * @throws BadRequestException
     */
    @Trivial
    protected String getReaderContents(InputStream input, int maxSize) throws IOException, BadRequestException {
        if (input == null) {
            // This should never happen, but if it does....
            throw new IOException("The input Reader was null");
        }
        try {
            byte[] buf = new byte[maxSize + 1];
            int read = input.read(buf);
            if (read > maxSize) {
                Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "POST_MAX_TEXT_SIZE", maxSize));
                throw new BadRequestException(MEDIA_TYPE_TEXT_PLAIN, error);
            } else if (read == -1) {
                Message error = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "POST_NO_PAYLOAD", maxSize));
                throw new BadRequestException(MEDIA_TYPE_TEXT_PLAIN, error);
            }
            return new String(buf, 0, read, "UTF-8");
        } finally {
            input.close();
        }
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
     * This is the default authorization.
     * Allow GET for reader/admin and POST/PUT/DELETE for admin only
     * This is used for V1Root, APIRoot, Catalog, Utils, FeatureUtils, URLUtils, Icons
     */
    public boolean isAuthorizedDefault(RESTRequest request, RESTResponse response) {
        boolean isGetMethod = "GET".equals(request.getMethod());

        /*
         * Authorize:
         * 1) principals with the administrator role, or
         * 2) principals with the reader role if the HTTP request method is GET.
         */
        boolean isAuthorized = request.isUserInRole(ADMINISTRATOR_ROLE_NAME)
                               || (isGetMethod && request.isUserInRole(READER_ROLE_NAME));

        if (!isAuthorized) {
            // Is not authorized, so build the error message.
            //response.sendError(403, "Forbidden");
            response.setStatus(HTTP_UNAUTHORIZED);
            response.setRequiredRoles(isGetMethod ? REQUIRED_ROLES_GET : REQUIRED_ROLES_DEFAULT);
        }

        return isAuthorized;
    }

    /**
     * This authorization allows admin and reader to access GET/POST/PUT/DELETE
     * This is used for Toolbox, Tooldata
     */
    protected boolean isAuthorizedAdminOrReader(RESTRequest request, RESTResponse response) {
        /*
         * Authorize:
         * 1) principals with the administrator or reader role
         */
        boolean isAuthorized = request.isUserInRole(ADMINISTRATOR_ROLE_NAME) || request.isUserInRole(READER_ROLE_NAME);

        if (!isAuthorized) {
            // Is not authorized, so build the error message.
            response.setStatus(HTTP_UNAUTHORIZED);
            response.setRequiredRoles(REQUIRED_ROLES_DEFAULT);
        }

        return isAuthorized;
    }

}
