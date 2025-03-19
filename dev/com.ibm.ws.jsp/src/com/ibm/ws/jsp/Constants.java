/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp;

import java.io.File;

public class Constants {
    public static final String JSP_SERVLET_BASE = "com.ibm.ws.jsp.runtime.HttpJspBase";
    public static final String SERVICE_METHOD_NAME = "_jspService";
    public static final String[] STANDARD_IMPORTS = {
                                                     "javax.servlet.*",
                                                     "javax.servlet.http.*",
                                                     "javax.servlet.jsp.*"
    };


    /* Wildcard is not needed since these are imported via jakarta.el.ImportHandler#importPackage */
    public static final String[] JAKARTA_STANDARD_IMPORTS = {
                                                            "jakarta.servlet",
                                                            "jakarta.servlet.http",
                                                            "jakarta.servlet.jsp"
    };

    public static final String[] STANDARD_JSP_EXTENSIONS = {
                                                            "*.jsp",
                                                            "*.jspx",
                                                            "*.jsw",
                                                            "*.jsv"
    };

    public static final String[] STANDARD_REQUIRED_JSP_JARS_FROM_LIB = {
                    "j2ee.jar"
    };
    public static final String[] STANDARD_REQUIRED_JSP_JARS = {
                    "com.ibm.ws.webcontainer_8.0.jar"
    };

    public static final int K = 1024;
    public static final int DEFAULT_BUFFER_SIZE = 8 * K;
    public static final int DEFAULT_TAG_BUFFER_SIZE = 8 * K;
    public static final long DEFAULT_RELOAD_INTERVAL = (1000 * 5); // 198832
    public static final boolean DEFAULT_RELOAD_ENABLED = true; // 198832
    public static final String PRECOMPILE = "jsp_precompile";
    public static final String JSP_PACKAGE_PREFIX = "_ibmjsp";
    public static final String JSP_FIXED_PACKAGE_NAME = "com.ibm._jsp";
    public static final String TAGFILE_PACKAGE_NAME = "com.ibm.ws.jsp.tagfile";
    public static final String OLD_JSP_PACKAGE_NAME = "org.apache.jsp";
    public static final String TAGFILE_PACKAGE_PATH = "com" + File.separator + "ibm" + File.separator + "ws" + File.separator + "jsp" + File.separator + "tagfile" + File.separator;

    public static final int PREPARE_JSPS_DEFAULT_THREADS = 1;
    public static final int PREPARE_JSPS_DEFAULT_MINLENGTH = 0;
    public static final int PREPARE_JSPS_DEFAULT_STARTAT = 0;

    public static final String FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri"; //PK81387
    public static final String INC_REQUEST_URI = "javax.servlet.include.request_uri";
    public static final String ASYNC_REQUEST_URI = "javax.servlet.async.request_uri";
    public static final String INC_SERVLET_PATH = "javax.servlet.include.servlet_path";
    public static final String INC_PATH_INFO = "javax.servlet.include.path_info";

    public static final String TMP_DIR = "javax.servlet.context.tempdir";

    public static final String TIMESTAMP_DELIMETER = "^";

    public static final String GENERATED_WEBXML_FILENAME = "generated_web.xml";

    public static final String OPEN_EXPR = "<%=";
    public static final String CLOSE_EXPR = "%>";
    public static final String OPEN_EXPR_XML = "%=";
    public static final String CLOSE_EXPR_XML = "%";
    public static final String FORWARD_SEEN = "javax.servlet.forward.seen";

    public static final String TAG_THREADPOOL_MAP = "com.ibm.ws.jsp.tagpoolmap";
    public static final String EXPRESSION_EVALUATOR = "com.ibm.ws.jsp.expreval";

    public static final String JSP_NAMESPACE = "http://java.sun.com/JSP/Page";
    /* Start Defect 202915 */
    public static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    /* End Defect 202915 */

    public static final String JSP_ROOT_TYPE = "root";
    public static final String JSP_TEXT_TYPE = "text";
    public static final String JSP_INCLUDE_DIRECTIVE_TYPE = "directive.include";
    public static final String JSP_PAGE_DIRECTIVE_TYPE = "directive.page";
    public static final String JSP_TAG_DIRECTIVE_TYPE = "directive.tag";
    public static final String JSP_ATTRIBUTE_DIRECTIVE_TYPE = "directive.attribute";
    public static final String JSP_VARIABLE_DIRECTIVE_TYPE = "directive.variable";
    public static final String JSP_DECLARATION_TYPE = "declaration";
    public static final String JSP_EXPRESSION_TYPE = "expression";
    public static final String JSP_SCRIPTLET_TYPE = "scriptlet";
    public static final String JSP_PARAM_TYPE = "param";
    public static final String JSP_PARAMS_TYPE = "params";
    public static final String JSP_FALLBACK_TYPE = "fallback";
    public static final String JSP_INCLUDE_TYPE = "include";
    public static final String JSP_FORWARD_TYPE = "forward";
    public static final String JSP_USEBEAN_TYPE = "useBean";
    public static final String JSP_GETPROPERTY_TYPE = "getProperty";
    public static final String JSP_SETPROPERTY_TYPE = "setProperty";
    public static final String JSP_PLUGIN_TYPE = "plugin";
    public static final String JSP_ATTRIBUTE_TYPE = "attribute";
    public static final String JSP_ELEMENT_TYPE = "element";
    public static final String JSP_BODY_TYPE = "body";
    public static final String JSP_INVOKE_TYPE = "invoke";
    public static final String JSP_DOBODY_TYPE = "doBody";
    public static final String JSP_OUTPUT_TYPE = "output";
    public static final String JSP_PAGE_CONTEXT_ORIG = "pageContext"; //PK65013
    public static final String JSP_PAGE_CONTEXT_NEW = "_jspx_page_context"; //PK65013
    public static final String JSP_EXPRESSION_FACTORY_OBJECT = "_jspx_ExpressionFactoryImplObject";
}