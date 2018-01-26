/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

/**
 * Constants for Java EE Security
 */
public class JavaEESecConstants {
    public static final String LOGIN_TO_CONTINUE_LOGINPAGE = "loginPage";
    public static final String LOGIN_TO_CONTINUE_ERRORPAGE = "errorPage";
    public static final String LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN = "useForwardToLogin";
    public static final String LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION = "useForwardToLoginExpression";
    public static final String LOGIN_TO_CONTINUE_USE_GLOBAL_LOGIN = "useGlobalLogin";
    public static final String LOGIN_TO_CONTINUE_LOGIN_FORM_CONTEXT_ROOT = "formLoginContextRoot";

    public static final String BASIC_AUTH_DEFAULT_REALM = "defaultRealm";

    public static final String SECURITY_CONTEXT_AUTH_PARAMS = "com.ibm.ws.security.javaeesec.auth.params";

    public static final String REALM_NAME = "realmName";
    public static final String CALLER_QUERY = "callerQuery";
    public static final String DS_LOOKUP = "dataSourceLookup";
    public static final String DEFAULT_DS_NAME = "java:comp/DefaultDataSource";
    public static final String GROUPS_QUERY = "groupsQuery";
    public static final String PWD_HASH_ALGORITHM = "hashAlgorithm";
    public static final String PWD_HASH_PARAMETERS = "hashAlgorithmParameters";
    public static final String PRIORITY = "priority";
    public static final String PRIORITY_EXPRESSION = "priorityExpression";
    public static final String USE_FOR = "useFor";
    public static final String USE_FOR_EXPRESSION = "useForExpression";

    public static final String GET_GROUPS_PERMISSION = "getGroups";
}
