/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* Temporary file pending public availability of api jar */
package javax.servlet.http;

import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.ServletResponse;

/**
 *
 */
public interface HttpServletResponse extends ServletResponse {
    // Field descriptor #5 I
    public static final int SC_CONTINUE = 100;

    // Field descriptor #5 I
    public static final int SC_SWITCHING_PROTOCOLS = 101;

    // Field descriptor #5 I
    public static final int SC_OK = 200;

    // Field descriptor #5 I
    public static final int SC_CREATED = 201;

    // Field descriptor #5 I
    public static final int SC_ACCEPTED = 202;

    // Field descriptor #5 I
    public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;

    // Field descriptor #5 I
    public static final int SC_NO_CONTENT = 204;

    // Field descriptor #5 I
    public static final int SC_RESET_CONTENT = 205;

    // Field descriptor #5 I
    public static final int SC_PARTIAL_CONTENT = 206;

    // Field descriptor #5 I
    public static final int SC_MULTIPLE_CHOICES = 300;

    // Field descriptor #5 I
    public static final int SC_MOVED_PERMANENTLY = 301;

    // Field descriptor #5 I
    public static final int SC_MOVED_TEMPORARILY = 302;

    // Field descriptor #5 I
    public static final int SC_FOUND = 302;

    // Field descriptor #5 I
    public static final int SC_SEE_OTHER = 303;

    // Field descriptor #5 I
    public static final int SC_NOT_MODIFIED = 304;

    // Field descriptor #5 I
    public static final int SC_USE_PROXY = 305;

    // Field descriptor #5 I
    public static final int SC_TEMPORARY_REDIRECT = 307;

    // Field descriptor #5 I
    public static final int SC_BAD_REQUEST = 400;

    // Field descriptor #5 I
    public static final int SC_UNAUTHORIZED = 401;

    // Field descriptor #5 I
    public static final int SC_PAYMENT_REQUIRED = 402;

    // Field descriptor #5 I
    public static final int SC_FORBIDDEN = 403;

    // Field descriptor #5 I
    public static final int SC_NOT_FOUND = 404;

    // Field descriptor #5 I
    public static final int SC_METHOD_NOT_ALLOWED = 405;

    // Field descriptor #5 I
    public static final int SC_NOT_ACCEPTABLE = 406;

    // Field descriptor #5 I
    public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

    // Field descriptor #5 I
    public static final int SC_REQUEST_TIMEOUT = 408;

    // Field descriptor #5 I
    public static final int SC_CONFLICT = 409;

    // Field descriptor #5 I
    public static final int SC_GONE = 410;

    // Field descriptor #5 I
    public static final int SC_LENGTH_REQUIRED = 411;

    // Field descriptor #5 I
    public static final int SC_PRECONDITION_FAILED = 412;

    // Field descriptor #5 I
    public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;

    // Field descriptor #5 I
    public static final int SC_REQUEST_URI_TOO_LONG = 414;

    // Field descriptor #5 I
    public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;

    // Field descriptor #5 I
    public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    // Field descriptor #5 I
    public static final int SC_EXPECTATION_FAILED = 417;

    // Field descriptor #5 I
    public static final int SC_INTERNAL_SERVER_ERROR = 500;

    // Field descriptor #5 I
    public static final int SC_NOT_IMPLEMENTED = 501;

    // Field descriptor #5 I
    public static final int SC_BAD_GATEWAY = 502;

    // Field descriptor #5 I
    public static final int SC_SERVICE_UNAVAILABLE = 503;

    // Field descriptor #5 I
    public static final int SC_GATEWAY_TIMEOUT = 504;

    // Field descriptor #5 I
    public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

    // Method descriptor #88 (Ljavax/servlet/http/Cookie;)V
    public abstract void addCookie(javax.servlet.http.Cookie arg0);

    // Method descriptor #90 (Ljava/lang/String;)Z
    public abstract boolean containsHeader(java.lang.String arg0);

    // Method descriptor #92 (Ljava/lang/String;)Ljava/lang/String;
    public abstract java.lang.String encodeURL(java.lang.String arg0);

    // Method descriptor #92 (Ljava/lang/String;)Ljava/lang/String;
    public abstract java.lang.String encodeRedirectURL(java.lang.String arg0);

    // Method descriptor #92 (Ljava/lang/String;)Ljava/lang/String; (deprecated)
    public abstract java.lang.String encodeUrl(java.lang.String arg0);

    // Method descriptor #92 (Ljava/lang/String;)Ljava/lang/String; (deprecated)
    public abstract java.lang.String encodeRedirectUrl(java.lang.String arg0);

    // Method descriptor #98 (ILjava/lang/String;)V
    public abstract void sendError(int arg0, java.lang.String arg1) throws java.io.IOException;

    // Method descriptor #101 (I)V
    public abstract void sendError(int arg0) throws java.io.IOException;

    // Method descriptor #103 (Ljava/lang/String;)V
    public abstract void sendRedirect(java.lang.String arg0) throws java.io.IOException;

    // Method descriptor #105 (Ljava/lang/String;J)V
    public abstract void setDateHeader(java.lang.String arg0, long arg1);

    // Method descriptor #105 (Ljava/lang/String;J)V
    public abstract void addDateHeader(java.lang.String arg0, long arg1);

    // Method descriptor #108 (Ljava/lang/String;Ljava/lang/String;)V
    public abstract void setHeader(java.lang.String arg0, java.lang.String arg1);

    // Method descriptor #108 (Ljava/lang/String;Ljava/lang/String;)V
    public abstract void addHeader(java.lang.String arg0, java.lang.String arg1);

    // Method descriptor #111 (Ljava/lang/String;I)V
    public abstract void setIntHeader(java.lang.String arg0, int arg1);

    // Method descriptor #111 (Ljava/lang/String;I)V
    public abstract void addIntHeader(java.lang.String arg0, int arg1);

    // Method descriptor #101 (I)V
    public abstract void setStatus(int arg0);

    // Method descriptor #98 (ILjava/lang/String;)V (deprecated)
    public abstract void setStatus(int arg0, java.lang.String arg1);

    // Method descriptor #115 ()I
    public abstract int getStatus();

    // Method descriptor #92 (Ljava/lang/String;)Ljava/lang/String;
    public abstract java.lang.String getHeader(java.lang.String arg0);

    // Method descriptor #118 (Ljava/lang/String;)Ljava/util/Collection;
    // Signature: (Ljava/lang/String;)Ljava/util/Collection<Ljava/lang/String;>;
    public abstract java.util.Collection getHeaders(java.lang.String arg0);

    // Method descriptor #122 ()Ljava/util/Collection;
    // Signature: ()Ljava/util/Collection<Ljava/lang/String;>;
    public abstract java.util.Collection getHeaderNames();

    public default void setTrailerFields(Supplier<Map<String, String>> supplier) throws IllegalStateException {

    }
}
