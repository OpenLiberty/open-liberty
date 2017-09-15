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
package com.ibm.wsspi.security.auth.callback;

/**
 * @author IBM Corporation
 * @ibm-spi
 */
public class Constants {

    // LoginModule sharedState
    public static final String CALLBACK_KEY = "Callback"; // Used to save Callback from CallbackHandler 
    public static final String WSPRINCIPAL_KEY = "WSPrincipal"; // Used to hold WSPrincipal 
    public static final String WSCREDENTIAL_KEY = "WSCredential"; // Used to hold WSCredential 
    public static final String WSAUTHTOKEN_KEY = "com.ibm.wsspi.security.token.wsAuthenticationToken"; // Used to hold the AuthenticationToken for attribute prop
    public static final String WSSSOTOKEN_KEY = "com.ibm.wsspi.security.token.wsSingleSignonToken"; // Used to hold the SingleSignonToken for attribute prop
    public static final String WSAUTHZTOKEN_KEY = "com.ibm.wsspi.security.token.wsAuthorizationToken"; // Used to hold the AuthorizationToken for attribute prop
    public static final String WSPROPTOKEN_KEY = "com.ibm.wsspi.security.token.wsPropagationToken"; // Used to hold the default PropagationToken for attribute prop
    public static final String WSKRBAUTHNTOKEN_KEY = "com.ibm.wsspi.security.token.wsKRBAuthnToken"; // Used to hold the optional Kerberos authentication Token for attribute prop
    public static final String WSOPAQUETOKEN_KEY = "WSOPAQUE"; // Used to hold the opaque token to be serialized outbound
    public static final String ALREADY_PROCESSED = "AlreadyProcessed ";

    // Application Context
    public static final String WEB_APP_NAME = "WebAppName"; // Used by both Web application and EJB application now
    public static final String REDIRECT_URL = "RedirectURL"; // Only used by Web application
    public static final String MODULE_NAME = "Module"; // Only used by EJB application
    public static final String COMPONENT_NAME = "Component"; // Only used by EJB application

    // Mapping properties
    public static final String MAPPING_ALIAS = "com.ibm.mapping.authDataAlias";
    public static final String USE_CALLER_IDENTITY = "com.ibm.mapping.useCallerIdentity";
    public static final String PROPAGATE_SEC_ATTRS = "com.ibm.mapping.propagateSecAttrs";
    public static final String TARGET_REALM_NAME = "com.ibm.mapping.targetRealmName";
    public static final String UNAUTHENTICATED_USER = "com.ibm.mapping.unauthenticatedUser";

}
