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
    public static final String JWT_BUILDER_CREATE_ENDPOINT = JWT_BUILDER_SERVLET + "/JwtBuilderCreateClient";
    public static final String JWT_BUILDER_SETAPIS_ENDPOINT = JWT_BUILDER_SERVLET + "/JwtBuilderSetApisClient";

    public static final String BUILT_JWT_TOKEN = "Built JWT Token: ";

    public static final String JWT_BUILDER_PARAM_BUILDER_ID = "configId";

    // since we can't pass null or "" as a parm to the app, we'll use some flags to indicate what we want
    public static final String NULL_STRING = "null";
    public static final String NULL_VALUE = null;
    public static final String EMPTY_STRING = "empty";
    public static final String EMPTY_VALUE = "";

}