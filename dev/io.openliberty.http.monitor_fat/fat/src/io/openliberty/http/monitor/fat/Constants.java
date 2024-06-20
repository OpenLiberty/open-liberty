/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

/**
 *
 */
public class Constants {

    //SERVLET

    public static final String SERVLET_APP = "ServletApp";

    public static final String SERVLET_CONTEXT_ROOT = "/ServletApp";

    public static final String WILDCARD_SERVLET_APP = "WildCardServlet";

    public static final String WILDCARD_APP_CONTEXT_ROOT = "/WildCardServlet";

    public static final String SIMPLE_SERVLET_URL = SERVLET_CONTEXT_ROOT + "/simpleServlet";

    public static final String FAIL_SERVLET_URL = SERVLET_CONTEXT_ROOT + "/failServlet";

    public static final String SUB1_SERVLET_URL = SERVLET_CONTEXT_ROOT + "/sub";

    public static final String SUB2_SERVLET_URL = SERVLET_CONTEXT_ROOT + "/sub/sub";

    //REST

    public static final String REST_APP = "RestApp";

    public static final String RETST_CONTEXT_ROOT = "/RestApp";

    public static final String REST_APP_URL = RETST_CONTEXT_ROOT + "/resource";

    public static final String SIMPLE_RESOURCE_URL = REST_APP_URL + "/simple";

    public static final String FAIL_RESOURCE_URL = REST_APP_URL + "/fail";

    public static final String PARAM_RESOURCE_URL = REST_APP_URL + "/params";

    //JSP
    static final String JSP_APP = "jspApp";
    static final String JSP_CONTEXT_ROOT = "/jspApp";

    //other
    static final String UNKOWN_SERVICE = "unknown_service:java";

    static final String RUNTIME_INSTANCE_SERVICE = "io.openliberty.microprofile.telemetry.runtime";
}
