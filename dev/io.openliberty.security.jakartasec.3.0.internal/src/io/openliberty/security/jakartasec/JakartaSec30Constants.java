/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

/**
 * Constants for Java EE Security
 */
public class JakartaSec30Constants extends JavaEESecConstants {

    public static final String OIDC_ANNOTATION = "oidc_annotation";

    public static final String BASE_URL_VARIABLE = "baseURL";

    public static final String BASE_URL_DEFAULT = "${" + BASE_URL_VARIABLE + "}/Callback";

    public static final String EMPTY_DEFAULT = "";

    public static final String SUBJECT_TYPE_SUPPORTED_DEFAULT = "public";

    public static final String RESPONSE_TYPE_CODE = "code";

    public static final String RESPONSE_TYPE_TOKEN = "token";

    public static final String RESPONSE_TYPE_SUPPORTED_DEFAULT = RESPONSE_TYPE_CODE + "," + OpenIdConstant.IDENTITY_TOKEN + "," + RESPONSE_TYPE_TOKEN + " "
                                                                 + OpenIdConstant.IDENTITY_TOKEN; //  "code,id_token,token id_token"

    public static final String NOT_BEFORE_IDENTIFIER = "nbf";

    public static final String ISSUED_AT_IDENTIFIER = "iat";

    public static final String JWT_ID_IDENTIFIER = "jti";

}