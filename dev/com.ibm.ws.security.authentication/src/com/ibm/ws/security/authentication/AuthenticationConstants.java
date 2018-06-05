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
package com.ibm.ws.security.authentication;

/**
 * The keys for the token configuration properties
 */
public interface AuthenticationConstants {

    /**
     * The unique id.
     */
    String UNIQUE_ID = "unique_id";

    /**
     * This key maps to the "authenticated userId" in a Subject's private credentials
     * hashtable. The "authenticated userId" is the String value returned from a call
     * to UserRegistry.checkPassword or UserRegistry.mapCertificate. This value may contain
     * additional information besides the securityName (e.g. SAF encodes the user's native
     * credential token in it), which may be needed later on by a credential provider. The
     * simplest way to pass this data to the credential provider is to hang it off the
     * Subject, like so.
     */
    String UR_AUTHENTICATED_USERID_KEY = "user.registry.authenticated.userid";

    /**
     * This key maps to a boolean property in a Subject's private credentials
     * hashtable. When the property is true, the authentication service will
     * authenticate a user with only the username supplied.
     */
    String INTERNAL_ASSERTION_KEY = "com.ibm.ws.authentication.internal.assertion";

    /**
     * This key maps to a JWT Web Token object in a Subject's private credentials
     * hashtable. When the property is set, the JWT Web token will be added in the subject
     */
    String INTERNAL_JSON_WEB_TOKEN = "com.ibm.ws.authentication.internal.json.web.token";

    String JASPI_PRINCIPAL = "com.ibm.wsspi.security.cred.jaspi.principal";

    /**
     * This key indicates the provider who authenticator and creator of the SSOToken. When this property is set, the key
     * and value will be added in the attribute of SSOToken.
     */
    String INTERNAL_AUTH_PROVIDER = "com.ibm.ws.authentication.internal.auth.provider";

    /**
     * This value indicates the internal authentication provider is set as the jaspic provider.
     */
    String INTERNAL_AUTH_PROVIDER_JASPIC = "jaspic";

    /**
     * This value indicates the INTERNAL_AUTH_PROVIDER_JASPIC is set as the jaspic form.
     * If this value is set, when a http request right after form login, the authentication will
     * be successful without invoking jaspi provider, then cookie will be deleted.
     * Note: This value should not go to the jwtSSOToken.
     */
    String INTERNAL_AUTH_PROVIDER_JASPIC_FORM = "jaspicForm";

    /**
     * This value indicates the INTERNAL_AUTH_PROVIDER_JASPIC is set as the jsr375form.
     * If this value is set, when a http request right after form login, the authentication will
     * invoke jaspi provider, then cookie will be deleted.
     * Note: This value should not go to the jwtSSOToken.
     */
    String INTERNAL_AUTH_PROVIDER_JSR375_FORM = "jsr375Form";
}
