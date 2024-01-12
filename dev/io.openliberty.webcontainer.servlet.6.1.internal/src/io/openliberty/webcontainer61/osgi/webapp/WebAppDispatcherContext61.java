/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.response.ResponseUtils;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

public class WebAppDispatcherContext61 extends WebAppDispatcherContext40 {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.osgi.webapp");
    protected static final String CLASS_NAME = WebAppDispatcherContext61.class.getName();

    public WebAppDispatcherContext61() {
        super();
    }

    public WebAppDispatcherContext61(WebApp webapp) {
        super(webapp);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "constructor", "this [" + this + "] , webapp [" + webapp + "]");
        }
    }

    public WebAppDispatcherContext61(IExtendedRequest req) {
        super(req);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "constructor", "this [" + this + "] , request [" + req + "]");
        }
    }

    public void sendRedirect(String location, int statusCode, boolean clearBuffer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", "this [" + this + "]");
        }

        HttpServletResponse response = (HttpServletResponse) getResponse();

        if (!response.isCommitted()) {
            if (!WCCustomProperties.REDIRECT_TO_RELATIVE_URL) {
                location = convertRelativeURIToURL(location);
            }

            response.setHeader("Location", location);
            response.setStatus(statusCode);

            if (clearBuffer) {
                String hyperText = ResponseUtils.encodeDataString(MessageFormat.format(nls.getString("sendRedirect.hyper.text.url"), new Object[] { location }));
                response.resetBuffer(); //clear underlying buffer without clearing headers and status code.

                try {
                    ServletOutputStream out = response.getOutputStream();
                    out.write(hyperText.getBytes("UTF-8"));
                } catch (IllegalStateException ise) {
                    PrintWriter writer = response.getWriter();
                    writer.print(hyperText);
                }
            }
        } else {
            throw new IllegalStateException(nls.getString("cannot.sendRedirect.response.already.committed"));
        }

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", "return from dispatcher.");
        }
    }
}
