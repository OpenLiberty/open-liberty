/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.context;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

public class ContextUtil {

    /**
     * Test Basic Variables
     */
    public static final String CLASSESNAME = "classes";
    public static final String SINGLETONSNAME = "singletons";
    public static final String CLASSESNAME2 = "classes2";
    public static final String SINGLETONSNAME2 = "singletons2";

    public static final String METHODNAME1 = "Bean";
    public static final String METHODNAME2 = "Field";
    public static final String METHODNAME3 = "Constructor";
    public static final String METHODNAME4 = "Param";
    public static final String METHODNAME5 = "NotBean";

    /**
     * Test data
     */
    public static final String ADDHEADERNAME = "ThreadLocalProviders";

    /**
     * URI and Request Name
     */
    public static final String URIINFONAME1 = "uriinfo1";
    public static final String URIINFONAME2 = "uriinfo2";
    public static final String URIINFONAME3 = "uriinfo3";
    public static final String URIINFONAME4 = "uriinfo4";
    public static final String URIINFONAME5 = "uriinfo5";

    public static final String HTTPHEADERSNAME1 = "httpheaders1";
    public static final String HTTPHEADERSNAME2 = "httpheaders2";
    public static final String HTTPHEADERSNAME3 = "httpheaders3";
    public static final String HTTPHEADERSNAME4 = "httpheaders4";
    public static final String HTTPHEADERSNAME5 = "httpheaders5";

    public static final String REQUESTNAME1 = "request1";
    public static final String REQUESTNAME2 = "request2";
    public static final String REQUESTNAME3 = "request3";
    public static final String REQUESTNAME4 = "request4";
    public static final String REQUESTNAME5 = "request5";

    public static final String RESOURCECONTEXT1 = "resourcecontext1";
    public static final String RESOURCECONTEXT2 = "resourcecontext2";
    public static final String RESOURCECONTEXT3 = "resourcecontext3";
    public static final String RESOURCECONTEXT4 = "resourcecontext4";
    public static final String RESOURCECONTEXT5 = "resourcecontext5";

    public static final String CONFIGNAME1 = "config1";
    public static final String CONFIGNAME2 = "config2";
    public static final String CONFIGNAME3 = "config3";
    public static final String CONFIGNAME4 = "config4";
    public static final String CONFIGNAME5 = "config5";

    public static String testUriInfo(UriInfo info) {
        StringBuilder buf = new StringBuilder();
        for (String param : info.getQueryParameters().keySet()) {
            buf.append(param);
            buf.append("\n");
        }
        return buf.toString();
    }

    public static String testHttpHeaders(HttpHeaders headers) {
        StringBuilder buf = new StringBuilder();
        for (String header : headers.getRequestHeaders().keySet()) {
            buf.append(header);
            //buf.append("\n");
            break;
        }
        return buf.toString();
    }

    public static String findHttpHeadersValue(HttpHeaders headers, String param) {
        String value = "";
        for (String header : headers.getRequestHeaders().keySet()) {
            if (header.equals(param)) {
                value = headers.getRequestHeaders().getFirst(header);
                break;
            }
        }
        return value;
    }
}
