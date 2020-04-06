/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal;

public interface OAuthConstants extends com.ibm.oauth.core.api.OAuthConstants {

    final static String UTF8 = "UTF-8";

    /*
     * Headers for requests and responses
     */
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String HTTP_CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String HTTP_CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    final static String HEADER_CACHE_CONTROL = "Cache-Control";
    final static String HEADERVAL_CACHE_CONTROL = "no-store";

    final static String HEADER_PRAGMA = "Pragma";
    final static String HEADERVAL_PRAGMA = "no-cache";

    /*
     * Other common attributes
     */
    final static String USERNAME = "username";
    final static String PASSWORD = "password";
    final static String STATE_ID = "state_id";
    final static String AUTHORIZED = "authorized";

    final static String HOST = "host";
    final static String PORT = "port";
    final static String METHOD = "method";
    final static String PATH = "path";
    final static String SCHEME = "scheme";
}
