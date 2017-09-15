/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

public class CommonWebConstants {

    public static final String UTF_8 = "UTF-8";

    public static final String HTTP_HEADER_ACCEPT = "Accept";
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_CONTENT_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String HTTP_CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    public final static String HEADER_CACHE_CONTROL = "Cache-Control";
    public final static String HEADER_PRAGMA = "Pragma";

    public final static String CACHE_CONTROL_NO_STORE = "no-store";
    public final static String PRAGMA_NO_CACHE = "no-cache";

    public static final String URI_PATH_CHARS_RAW = "a-zA-Z0-9._~%!$&'()*+,;=:@/-";
    public static final String VALID_URI_PATH_CHARS = "[" + URI_PATH_CHARS_RAW + "]";
    public static final String VALID_URI_QUERY_CHARS = "[a-zA-Z0-9._~%!$&'()*+,;=:@/?-]";

}
