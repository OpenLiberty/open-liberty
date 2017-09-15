/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

public interface HttpServiceConstants {

    public static final String TOPIC_PFX = "com/ibm/ws/transport/http/endpoint/";
    public static final String ENDPOINT_ACTIVE_PORT = "activePort";
    public static final String ENDPOINT_CONFIG_HOST = "configHost";
    public static final String ENDPOINT_CONFIG_PORT = "configPort";
    public static final String ENDPOINT_NAME = "name";
    public static final String ENDPOINT_IS_HTTPS = "isHttps";
    public static final String ENDPOINT_EXCEPTION = "exception";

    public static final String ENDPOINT_FAILED = "/FAILED";
    public static final String ENDPOINT_STARTED = "/STARTED";
    public static final String ENDPOINT_STOPPED = "/STOPPED";
    public static final String ENPOINT_FPID_ALIAS = "httpEndpoint";
    public static final String ENDPOINT_FPID = "com.ibm.ws.http";

    public static final String VHOST_FPID_ALIAS = "virtualHost";
    public static final String VHOST_FPID = "com.ibm.ws.http.virtualhost";
    public static final String VHOST_HOSTALIAS = "hostAlias";
    public static final String VHOST_ALLOWED_ENDPOINT = "allowFromEndpointRef";

    public static final String DEFAULT_VHOST = "default_host";
    public static final String ENABLED = "enabled";

    public static final String WILDCARD = "*";
    public static final String LOCALHOST = "localhost";
    public static final String DEFAULT_PORT = "80";
    public static final String DEFAULT_HOSTNAME = "_defaultHostName";

}
