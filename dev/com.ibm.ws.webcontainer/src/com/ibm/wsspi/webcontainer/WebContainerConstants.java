/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import com.ibm.ws.webcontainer.webapp.WebApp;

/**
 *
 * 
 * WebContainerConstants contains static final strings to be used by all of 
 * webcontainer to prevent string recreation.
 * @ibm-private-in-use
 */

public interface WebContainerConstants {
    public static final String INCLUDE = "include";
    public static final String FORWARD = "forward";
    public static final String ASYNC = "async";
    public static final String NESTED_TRUE = "true";

    public static final String IGNORE_DISPATCH_STATE = "com.ibm.wsspi.webcontainer.ignoreDispatchState";
    public static final String DISPATCH_TYPE_ATTR = "com.ibm.servlet.engine.webapp.dispatch_type";
    public static final String DISPATCH_NESTED_ATTR = "com.ibm.servlet.engine.webapp.dispatch_nested";

    public static final String JAVAX_SERVLET_REQUEST_SSL_SESSION_ID = "javax.servlet.request.ssl_session_id";

    public static final String SERVLET_PATH_FORWARD_ATTR = "javax.servlet.forward.servlet_path";
    public static final String REQUEST_URI_FORWARD_ATTR = "javax.servlet.forward.request_uri";
    public static final String CONTEXT_PATH_FORWARD_ATTR = "javax.servlet.forward.context_path";
    public static final String PATH_INFO_FORWARD_ATTR = "javax.servlet.forward.path_info"; 
    public static final String QUERY_STRING_FORWARD_ATTR = "javax.servlet.forward.query_string";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String SERVLET_PATH_INCLUDE_ATTR = "javax.servlet.include.servlet_path";
    public static final String REQUEST_URI_INCLUDE_ATTR = "javax.servlet.include.request_uri";
    public static final String CONTEXT_PATH_INCLUDE_ATTR = "javax.servlet.include.context_path";
    public static final String PATH_INFO_INCLUDE_ATTR = "javax.servlet.include.path_info"; 
    public static final String QUERY_STRING_INCLUDE_ATTR = "javax.servlet.include.query_string"; 

    public static final String SERVLET_PATH_ASYNC_ATTR = "javax.servlet.async.servlet_path";
    public static final String REQUEST_URI_ASYNC_ATTR = "javax.servlet.async.request_uri";
    public static final String CONTEXT_PATH_ASYNC_ATTR = "javax.servlet.async.context_path";
    public static final String PATH_INFO_ASYNC_ATTR = "javax.servlet.async.path_info"; 
    public static final String QUERY_STRING_ASYNC_ATTR = "javax.servlet.async.query_string";

    // For Servlet 4.0 (values will only bet set when servlet 4.0 is enabled)
    public static final String MAPPING_INCLUDE_ATTR = "javax.servlet.include.mapping";
    public static final String MAPPING_FORWARD_ATTR = "javax.servlet.forward.mapping";
    public static final String MAPPING_ASYNC_ATTR = "javax.servlet.async.mapping";


    public static final String [] WEBCONTAINER_REQ_ATTRS = {DISPATCH_TYPE_ATTR,DISPATCH_NESTED_ATTR,SERVLET_PATH_FORWARD_ATTR,
                                                            REQUEST_URI_FORWARD_ATTR, CONTEXT_PATH_FORWARD_ATTR, PATH_INFO_FORWARD_ATTR, QUERY_STRING_FORWARD_ATTR,
                                                            SERVLET_PATH_INCLUDE_ATTR, REQUEST_URI_INCLUDE_ATTR, CONTEXT_PATH_INCLUDE_ATTR,PATH_INFO_INCLUDE_ATTR,
                                                            QUERY_STRING_INCLUDE_ATTR};

    public static final String [] WEBCONTAINER_FORWARD_ATTRS = {SERVLET_PATH_FORWARD_ATTR,
                                                                REQUEST_URI_FORWARD_ATTR, CONTEXT_PATH_FORWARD_ATTR, PATH_INFO_FORWARD_ATTR, QUERY_STRING_FORWARD_ATTR};

    public static final String [] WEBCONTAINER_INCLUDE_ATTRS = {SERVLET_PATH_INCLUDE_ATTR, REQUEST_URI_INCLUDE_ATTR,
                                                                CONTEXT_PATH_INCLUDE_ATTR,PATH_INFO_INCLUDE_ATTR, QUERY_STRING_INCLUDE_ATTR};

    public static final String WEBCONTAINER_ACCEPTOR_ID = "com.ibm.ws.runtime.WebContainerImpl";

    public static final String FILTER_PROXY_MAPPING = WebApp.FILTER_PROXY_MAPPING;

    public static final String FILTER_FILENOTFOUND_ATTR = "com.ibm.ws.webcontainer.filter.filenotfound";

    public static int REGISTER_EXTFACTORY_BEFORE_COLLABORATOR_HELPER = 0;

    public static final String WRITE_TYPE_OVERRIDE="com.ibm.wsspi.webcontainer.write.type.override";
    public static final String TEXT = "text";
    public static final String CHARSET_EQUALS = "charset=";
    //ImportSupport in jstl also uses the Value from this constant 
    public static final String DYNACACHE_REQUEST_ATTR_KEY = "com.ibm.ws.cache.flush.imports";
    public static final String FINISHED_FITLERS_WITHOUT_TARGET = "FINISHED_FITLERS_WITHOUT_TARGET";

    public static final String X_POWERED_BY_KEY = "X-Powered-By";
    public static final String X_POWERED_BY_DEFAULT_VALUE = "Servlet/3.0";

    // RTC 160610. Moving to com.ibm.websphere.servlet.container.WebContainer
    // to avoid having to export this class (WebContainerConstants).
    //public static enum Feature {RRD,ARD};
}
