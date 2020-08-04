/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;

public class JWTBuilderConstants extends JwtConstants {

    public static final String NEW_LINE = System.getProperty("line.separator");

    /*********************************** Builder Defaults *************************************/
    public static final String JWT_BUILDER_DEFAULT_CONFIG = "defaultJwtBuilder";

    /********************************* JWT Builder API Servlet ********************************/
    public static final String JWT_BUILDER_SERVLET = "jwtbuilderclient";
    public static final String JWT_BUILDER_PROTECTED_SERVLET = "jwtprotectedbuilderclient";
    public static final String JWT_BUILDER_CREATE_ENDPOINT = JWT_BUILDER_SERVLET + "/JwtBuilderCreateClient";
    public static final String JWT_BUILDER_SETAPIS_ENDPOINT = JWT_BUILDER_SERVLET + "/JwtBuilderSetApisClient";
    public static final String JWT_BUILDER_PROTECTED_SETAPIS_ENDPOINT = JWT_BUILDER_PROTECTED_SERVLET + "/JwtBuilderSetApisClient";

    public static final String JWT_BUILDER_ACTION_CLAIM_FROM = "claim_from";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN = "claim_from_JwtToken";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING = "claim_from_JwtString";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL = "claim_from_JwtToken_null";
    public static final String JWT_BUILDER_CLAIM_API = "claim_api";
    public static final String JWT_BUILDER_REMOVE_API = "remove_api";
    public static final String JWT_BUILDER_CLAIMFROM_API = "claimFrom_api";
    public static final String JWT_BUILDER_FETCH_API = "fetch_api";
    public static final String ADD_CLAIMS_AS = "add_claim_as";
    public static final String AS_COLLECTION = "collection";
    public static final String AS_SINGLE = "single";
    public static final String JWT_TOKEN = "jwt_token";
    public static final String SHARED_KEY = "shared_key";
    public static final String SHARED_KEY_TYPE = "shared_key_type";
    public static final String SHARED_KEY_STRING_TYPE = "string";
    public static final String SHARED_KEY_PRIVATE_KEY_TYPE = "priviate_key";
    public static final String SHARED_KEY_PUBLIC_KEY_TYPE = "public_key";

    public static final String BUILT_JWT_TOKEN = "Built JWT Token: ";

    public static final String JWT_BUILDER_PARAM_BUILDER_ID = "configId";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String WSPRINCIPAL = "WSPrincipal:";

    // since we can't pass null or "" as a parm to the app, we'll use some flags to indicate what we want
    public static final String NULL_STRING = "null";
    public static final String NULL_VALUE = null;
    public static final String EMPTY_STRING = "empty";
    public static final String EMPTY_VALUE = "";

}