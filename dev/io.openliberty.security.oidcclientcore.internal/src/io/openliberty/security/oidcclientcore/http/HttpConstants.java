/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

public interface HttpConstants {

    public final static String HTTP_SCHEME = "http:";
    public final static String HTTPS_SCHEME = "https:";

    public final static String AUTHORIZATION = "Authorization";
    public final static String BASIC = "Basic ";
    public final static String BEARER = "Bearer ";

    public final static String UTF_8 = "UTF-8";

    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_JWT = "application/jwt";

    public final static String METHOD_BASIC = "basic";
    public final static String METHOD_POST = "post";

    public final static String RESPONSEMAP_CODE = "RESPONSEMAP_CODE";
    public final static String RESPONSEMAP_METHOD = "RESPONSEMAP_METHOD";

}
