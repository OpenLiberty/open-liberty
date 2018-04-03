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

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * This interface represents the data to be used during authentication.
 * The authentication data are used by the AuthenticationService in order to
 * authenticate. The class provides for protecting sensitive data from being displayed
 * in traces when the key being used is identified as a sensitive one.
 */
public interface AuthenticationData {

    /**
     * The key used to set a username given a String.
     */
    public static final String USERNAME = "USERNAME";

    /**
     * The key used to set the password given a char[]. Data provided using this key
     * is considered sensitive.
     */
    public static final String PASSWORD = "PASSWORD";

    /**
     * The key used to set a token given a byte[]. Data provided using this key is
     * considered sensitive. TODO: Follow-up if a token is really sensitive
     */
    public static final String TOKEN = "TOKEN";

    /**
     * The key used to set a token given a String. Data provided using this key is
     * considered sensitive. TODO: Follow-up if a token is really sensitive
     */
    public static final String TOKEN64 = "TOKEN64";

    /**
     * The key used to set a cert chain given an X509Certificate[]. Data provided using this key is
     * considered sensitive. TODO: Follow-up if a certs are really sensitive
     */
    public static final String CERTCHAIN = "CERTCHAIN";

    /**
     * The key used to set a realm name given a String.
     */
    public static final String REALM = "REALM";

    /**
     * The key used to set an HTTP servlet request given an HttpServletRequest.
     */
    public static final String HTTP_SERVLET_REQUEST = "HTTP_SERVLET_REQUEST";

    /**
     * The key used to set an HTTP servlet response given an HttpServletResponse.
     */
    public static final String HTTP_SERVLET_RESPONSE = "HTTP_SERVLET_RESPONSE";

    /**
     * The key used to set an application context map given a Map.
     */
    public static final String APPLICATION_CONTEXT = "APPLICATION_CONTEXT";

    /**
     * The key used to set an authentication mechanism OID given a String.
     */
    public static final String AUTHENTICATION_MECH_OID = "AUTHENTICATION_MECH_OID";

    public static final String JWT_TOKEN = "JWT_TOKEN";

    /**
     * Sets the key-value pair.
     *
     * @param key
     * @param value
     */
    public void set(String key, @Sensitive Object value);

    /**
     * Retrieves the stored value for the key.
     *
     * @param key
     * @return
     */
    @Sensitive
    public Object get(String key);
}
