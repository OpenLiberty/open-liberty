/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.social.oidc.certification;

import com.ibm.ws.security.fat.common.social.SocialConstants;

public class Constants extends SocialConstants {

    public static final String CERTIFICATION_RP_ID = "open-liberty";

    public static final String RP_KEY_CLIENT_ID = "client_id";
    public static final String RP_KEY_CLIENT_SECRET = "client_secret";

    public static final String OP_KEY_REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String OP_KEY_AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String OP_KEY_TOKEN_ENDPOINT = "token_endpoint";
    public static final String OP_KEY_USER_INFO_ENDPOINT = "userinfo_endpoint";
    public static final String OP_KEY_JWKS_URI = "jwks_uri";
    public static final String OP_KEY_SCOPES_SUPPORTED = "scopes_supported";

    public static final String CONFIG_VAR_CLIENT_ID = "oidc.certification.clientId";
    public static final String CONFIG_VAR_CLIENT_SECRET = "oidc.certification.clientSecret";
    public static final String CONFIG_VAR_SCOPE = "oidc.certification.scope";
    public static final String CONFIG_VAR_AUTHORIZATION_ENDPOINT = "oidc.certification.authorizationEndpoint";
    public static final String CONFIG_VAR_TOKEN_ENDPOINT = "oidc.certification.tokenEndpoint";
    public static final String CONFIG_VAR_USER_INFO_ENDPOINT = "oidc.certification.userInfoEndpoint";
    public static final String CONFIG_VAR_JWKS_URI = "oidc.certification.jwksUri";
    public static final String CONFIG_VAR_SIGNATURE_ALGORITHM = "oidc.certification.signatureAlgorithm";
    public static final String CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD = "oidc.certification.tokenEndpointAuthMethod";
    public static final String CONFIG_VAR_USER_INFO_ENDPOINT_ENABLED = "oidc.certification.userInfoEndpointEnabled";

    public static final String CLIENT_REGISTRATION_KEY_REDIRECT_URIS = "redirect_uris";
    public static final String CLIENT_REGISTRATION_KEY_CONTACTS = "contacts";
    public static final String CLIENT_REGISTRATION_KEY_TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";

}
