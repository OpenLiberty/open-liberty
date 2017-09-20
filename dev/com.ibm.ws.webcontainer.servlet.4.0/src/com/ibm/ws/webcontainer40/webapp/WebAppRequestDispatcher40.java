/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.webapp;

import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;

import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.ws.webcontainer40.osgi.webapp.WebApp40;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * There is a certain degree of code duplication in this class for the purpose
 * of performance gains
 *
 * RequestDispatcher implementation
 */
public class WebAppRequestDispatcher40 extends WebAppRequestDispatcher {

    String old_mapping = null;
    String old_mapping_for = null;
    String old_mapping_async = null;

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer40.webapp");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer40.webapp.WebAppRequestDispatcher40";

    /**
     * @param app
     * @param path
     * @param dispatchContext
     *
     *            This constructor will be called by the getRequestDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher40(WebApp40 app, String path) // PK07351 removed
    {
        super(app, path);
    }

    /**
     * @param app
     * @param path
     * @param isAsync
     * @param dispatchContext
     *
     *            This constructor will be called by the getRequestDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher40(WebApp40 app, String path, boolean isAsync) // PK07351
    {
        super(app, path, isAsync);
    }

    /**
     * Constructor WebAppRequestDispatcher.
     *
     * @param webApp
     * @param p
     *
     *            This constructor will be called by the getNamedDispatcher()
     *            method in WebApp
     */
    public WebAppRequestDispatcher40(WebApp40 webApp, RequestProcessor p) {
        super(webApp, p);
    }

    @Override
    protected void setAttributes(ServletRequest request, DispatcherType dispatcherType, String requestURI, String servletPath, String pathInfo,
                                 String contextPath, String queryString, String dispatchMapping) {

        if (dispatcherType == DispatcherType.INCLUDE) {
            request.setAttribute(MAPPING_INCLUDE_ATTR, dispatchMapping);
        } else if (dispatcherType == DispatcherType.FORWARD) {
            request.setAttribute(MAPPING_FORWARD_ATTR, dispatchMapping);
        } else if (dispatcherType == DispatcherType.ASYNC) {
            request.setAttribute(MAPPING_ASYNC_ATTR, dispatchMapping);
        }
        super.setAttributes(request, dispatcherType, requestURI, servletPath, pathInfo, contextPath, queryString, null);

    }

    @Override
    protected void clearAttributes(ServletRequest request, DispatcherType dispatcherType) {

        if (dispatcherType == DispatcherType.INCLUDE) {
            request.removeAttribute(MAPPING_INCLUDE_ATTR);
        } else if (dispatcherType == DispatcherType.FORWARD) {
            request.removeAttribute(MAPPING_FORWARD_ATTR);
        } else if (dispatcherType == DispatcherType.ASYNC) {
            request.removeAttribute(MAPPING_ASYNC_ATTR);
        }
        super.clearAttributes(request, dispatcherType);

    }

}
