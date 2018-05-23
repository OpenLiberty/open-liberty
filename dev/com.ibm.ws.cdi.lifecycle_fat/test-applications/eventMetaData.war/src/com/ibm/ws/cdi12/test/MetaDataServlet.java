/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/")
public class MetaDataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static EventMetadata beanStartMetaData = null;
    private static EventMetadata beanFiredMetaData = null;

    @Inject
    private RequestScopedBean bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        bean.fireEvent();

        PrintWriter pw = resp.getWriter();

        String qualifiers = "null";
        String injectionPoint = "null";
        String type = "null";

        if (beanStartMetaData.getQualifiers() != null) {
            qualifiers = beanStartMetaData.getQualifiers().toString();
        }
        if (beanStartMetaData.getInjectionPoint() != null) {
            injectionPoint = beanStartMetaData.getInjectionPoint().toString();
        }
        if (beanStartMetaData.getType() != null) {
            type = beanStartMetaData.getType().toString();
        }

        pw.append("Default event qualifiers: " + qualifiers + System.lineSeparator());
        pw.append("Default event injection points: " + injectionPoint + System.lineSeparator());
        pw.append("Default event type: " + type + System.lineSeparator());

        qualifiers = "null";
        injectionPoint = "null";
        type = "null";

        if (beanFiredMetaData.getQualifiers() != null) {
            qualifiers = beanFiredMetaData.getQualifiers().toString();
        }
        if (beanFiredMetaData.getInjectionPoint() != null) {
            injectionPoint = beanFiredMetaData.getInjectionPoint().toString();
        }
        if (beanFiredMetaData.getType() != null) {
            type = beanFiredMetaData.getType().toString();
        }

        pw.append("Non-default event qualifiers: " + qualifiers + System.lineSeparator());
        pw.append("Non-default event injection points: " + injectionPoint + System.lineSeparator());
        pw.append("Non-default event type: " + type + System.lineSeparator());

    }

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {
        if (beanStartMetaData == null) {
            beanStartMetaData = em;
        }
    }

    public static void onFired(@Observes MyEvent e, EventMetadata em) {
        if (beanFiredMetaData == null) {
            beanFiredMetaData = em;
        }
    }

}
