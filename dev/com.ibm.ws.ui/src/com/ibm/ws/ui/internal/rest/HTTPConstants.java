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

/**
 * <p>The set of HTTP constants we will use.</p>
 *
 * <p>Please be very careful when adding to this class. New constants require
 * scrutiny because it affects our externals.</p>
 */
public interface HTTPConstants {

    /**
     * HTTP status code 200. Default "request complete" response.
     */
    int HTTP_OK = 200;

    /**
     * HTTP status code 201. Indicates the requested POST completed
     * successfully and the requested resource was created.
     */
    int HTTP_CREATED = 201;

    /**
     * HTTP status code 204. Indicates the GET requested has been successfully fulfilled and
     * that there is no additional content to send in the response payload body.
     */
    int HTTP_NO_CONTENT = 204;

    /**
     * <p>HTTP status code 400. Indicates to the caller that the request was
     * malformed or in some other way 'bad'.</p>
     * <p>Can be used to mean "I don't understand what you are asking".</p>
     */
    int HTTP_BAD_REQUEST = 400;

    /**
     * HTTP status code 401. Indicates the resource is protected and that the
     * caller is unauthenticated.
     */
    int HTTP_UNAUTHENTICATED = 401;

    /**
     * HTTP status code 403. Indicates the resource is protected and that the
     * caller is not authorized to access the resource.
     */
    int HTTP_UNAUTHORIZED = 403;

    /**
     * HTTP status code 404. Indicates the requested resource is not defined.
     */
    int HTTP_NOT_FOUND = 404;

    /**
     * HTTP status code 405. Indicates the HTTP method (GET, POST, PUT, DELETE)
     * is not supported by the resource.
     */
    int HTTP_METHOD_NOT_SUPPORTED = 405;

    /**
     * HTTP status code 409. Indicates the requested resource to create already
     * exists.
     */
    int HTTP_CONFLICT = 409;

    /**
     * HTTP status code 415. Indicates the requested media type is not
     * supported by the resource.
     */
    int HTTP_MEDIA_TYPE_NOT_SUPPORTED = 415;

    /**
     * HTTP status code 412. Indicates the requested is missing pre-conditions.
     */
    int HTTP_PRECONDITION_FAILED = 412;

    /**
     * HTTP status code 500. Indicates a non-recoverable error occurred while
     * processing the request.
     */
    int HTTP_INTERNAL_ERROR = 500;

    /**
     * HTTP status code 503. Indicates an underlying component is missing, and
     * therefore the request could not be completed.
     */
    //protected static int HTTP_INTERNAL_ERROR = 503;

    /**
     * HTTP method GET.
     */
    String HTTP_METHOD_GET = "GET";

    /**
     * HTTP method POST.
     */
    String HTTP_METHOD_POST = "POST";

    /**
     * HTTP method PUT.
     */
    String HTTP_METHOD_PUT = "PUT";

    /**
     * HTTP method DELETE.
     */
    String HTTP_METHOD_DELETE = "DELETE";

    /**
     * HTTP header entity tag.
     */
    String HTTP_HEADER_ETAG = "ETag";

    /**
     * HTTP header If-Match
     */
    String HTTP_HEADER_IF_MATCH = "If-Match";

    /**
     * HTTP header indicating the content type.
     */
    String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * HTTP response header content type of JSON.
     */
    String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    /**
     * HTTP request header content type of JSON.
     */
    String MEDIA_TYPE_APPLICATION_JSON_NO_CHARSET = "application/json";

    /**
     * HTTP header content type of png image.
     */
    String MEDIA_TYPE_IMAGE_PNG = "image/png";

    /**
     * HTTP header content type of jpeg image.
     */
    String MEDIA_TYPE_IMAGE_JPG = "image/jpeg";

    /**
     * HTTP header content type of gif image.
     */
    String MEDIA_TYPE_IMAGE_GIF = "image/gif";

    /**
     * HTTP header content type of plain text.
     */
    String MEDIA_TYPE_TEXT_PLAIN = "text/plain";

}
