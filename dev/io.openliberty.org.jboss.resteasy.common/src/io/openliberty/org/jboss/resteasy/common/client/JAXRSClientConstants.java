/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

public class JAXRSClientConstants {

    public final static String SSL_REFKEY = "com.ibm.ws.jaxrs.client.ssl.config";
    public final static String CONNECTION_TIMEOUT = "com.ibm.ws.jaxrs.client.connection.timeout";
    public final static String RECEIVE_TIMEOUT = "com.ibm.ws.jaxrs.client.receive.timeout";
    public final static long TIMEOUT_DEFAULT = 30000;
    public final static String PROXY_HOST = "com.ibm.ws.jaxrs.client.proxy.host";
    public final static String PROXY_PORT = "com.ibm.ws.jaxrs.client.proxy.port";
    public final static String PROXY_TYPE = "com.ibm.ws.jaxrs.client.proxy.type";
    public final static String PROXY_AUTH_TYPE = "com.ibm.ws.jaxrs.client.proxy.authType";
    public final static String PROXY_AUTH_TYPE_DEFAULT = "Basic";
    public final static String PROXY_USERNAME = "com.ibm.ws.jaxrs.client.proxy.username";
    public final static String PROXY_PASSWORD = "com.ibm.ws.jaxrs.client.proxy.password";
    public final static int PROXY_PORT_DEFAULT = 80;
    public final static String LTPA_HANDLER = "com.ibm.ws.jaxrs.client.ltpa.handler";
    public static final String OAUTH_HANDLER = "com.ibm.ws.jaxrs.client.oauth.sendToken";
    public static final String JWT_HANDLER = "com.ibm.ws.jaxrs.client.oidc.sendJwtToken";
    public static final String MPJWT_HANDLER = "com.ibm.ws.jaxrs.client.mpjwt.sendToken";
    public static final String DISABLE_CN_CHECK = "com.ibm.ws.jaxrs.client.disableCNCheck";
    public final static String SAML_HANDLER = "com.ibm.ws.jaxrs.client.saml.sendToken";
    public final static String AUTO_FOLLOW_REDIRECTS = "io.openliberty.rest.client.autoFollowRedirects";

}
