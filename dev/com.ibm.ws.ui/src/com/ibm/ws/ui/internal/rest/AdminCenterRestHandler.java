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

import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>The AdminCenterRestHandler is the interface which all of the Admin Center REST
 * resource handlers should implement. An Admin Center REST Resources may or
 * may not have direct children. If they have children, they may have grandchildren.</p>
 * <p>For example, if the AdminCenterRestHandler is handling /adminCenter/catalog, then:
 * /adminCenter/catalog/tool/toolA - tool is a child, and toolA is a grandchild.
 * Even deeper grandchild are permitted and are specially handled by the implementor.</p>
 * <p>The common implementation of the AdminCenterRestHandler is {@link CommonJSONRESTHandler}.
 * This abstract implementation will handle all of the common flows and default
 * behaviours as required by the Admin Center REST API design.</p>
 */
public interface AdminCenterRestHandler extends RESTHandler {

    /**
     * Simple container for replying to a POST request. Need to indicate both
     * created URL and the JSON response payload.
     */
    class POSTResponse {
        /**
         * The URL created with the POST request.
         */
        public String createdURL;

        /**
         * The payload to return. This is optional can may be null.
         */
        public Object jsonPayload;
    }

    /**
     * Indicates the base URL which the handler handles. The handler may or may
     * not have child resources.
     * 
     * @return The base resource which the handler handles.
     */
    String baseURL();

    /**
     * Indicates whether or not the handler has any children.
     * Handlers which do not have children will not be matched for child URLs
     * and are safe-guarded against being invoked to handle children.
     * 
     * @return {@code true} if the handler expects any children, {@code false} otherwise
     */
    boolean hasChildren();

    /**
     * Indicates whether or not the handler has any grandchildren.
     * Handlers which do not have grandchildren will not be matched for
     * grandchild URLs and are safe-guarded against being invoked to
     * handle grandchildren.
     * 
     * @return {@code true} if the handler expects any grandchildren, {@code false} otherwise
     */
    boolean hasGrandchildren();

    /**
     * Indicates whether or not the handler recognizes the specified child
     * resource. If the handler has no children, this method is not invoked.
     * If the handler does not recognize the child, a 404 is returned. If the
     * handler does recognize the child, but a method to handle the request has
     * not been defined, a 405 is returned.
     * 
     * @param child The child resource to determine if the handler recognize
     * @param request The RESTRequest
     * @return {@code true} if the handler recognizes the child, {@code false} otherwise.
     */
    boolean isKnownChildResource(String child, RESTRequest request);

    /**
     * Indicates whether or not the handler recognizes the specified child
     * resource. If the handler has no children, this method is not invoked.
     * If the handler does not recognize the child, a 404 is returned. If the
     * handler does recognize the child, but a method to handle the request has
     * not been defined, a 405 is returned.
     * 
     * @param child The child resource to determine if the handler recognize
     * @param request The RESTRequest
     * @return {@code true} if the handler recognizes the child, {@code false} otherwise.
     */
    boolean isKnownGrandchildResource(String child, String grandchild, RESTRequest request);

    /**
     * <p>Handles the GET method on the base resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request
     * @param response
     * @return The JSON object to return in the response to the GET request
     */
    Object getBase(RESTRequest request, RESTResponse response) throws RESTException;

    /**
     * <p>Handles the GET method on a child of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @return The JSON object to return in the response to the GET request
     */
    Object getChild(RESTRequest request, RESTResponse response, String child) throws RESTException;

    /**
     * <p>Handles the GET method on a grandchild of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @param grandchild The name of the grandchild (child's child) being acted upon
     * @return The JSON object to return in the response to the GET request
     */
    Object getGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException;

    /**
     * <p>Handles the POST method on the base resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @return The POSTResponse object to return in response to the POST request
     */
    POSTResponse postBase(RESTRequest request, RESTResponse response) throws RESTException;

    /**
     * <p>Handles the POST method on a child of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @return The POSTResponse object to return in response to the POST request
     */
    POSTResponse postChild(RESTRequest request, RESTResponse response, String child) throws RESTException;

    /**
     * <p>Handles the POST method on a grandchild of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @param grandchild The name of the grandchild (child's child) being acted upon
     * @return The JSON object to return in the response to the POST request
     */
    POSTResponse postGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException;

    /**
     * <p>Handles the PUT method on the base resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @return The JSON object to return in the response to the PUT request
     */
    Object putBase(RESTRequest request, RESTResponse response) throws RESTException;

    /**
     * <p>Handles the PUT method on a child of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @return The JSON object to return in the response to the PUT request
     */
    Object putChild(RESTRequest request, RESTResponse response, String child) throws RESTException;

    /**
     * <p>Handles the PUT method on a grandchild of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @param grandchild The name of the grandchild (child's child) being acted upon
     * @return The JSON object to return in the response to the PUT request
     */
    Object putGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException;

    /**
     * <p>Handles the DELETE method on the base resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @return The JSON object to return in the response to the DELETE request
     */
    Object deleteBase(RESTRequest request, RESTResponse response) throws RESTException;

    /**
     * <p>Handles the DELETE method on a child of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @return The JSON object to return in the response to the DELETE request
     */
    Object deleteChild(RESTRequest request, RESTResponse response, String child) throws RESTException;

    /**
     * <p>Handles the DELETE method on a grandchild of the resource.</p>
     * 
     * <p>By default, the method is not supported. Implementors should override
     * this implementation to provide supported behaviour.</p>
     * 
     * @param request The RESTRequest from handleRequest
     * @param response The RESTResponse from handleRequest
     * @param child The name of the child resource being acted upon
     * @param grandchild The name of the grandchild (child's child) being acted upon
     * @return The JSON object to return in the response to the DELETE request
     */
    Object deleteGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException;
}
