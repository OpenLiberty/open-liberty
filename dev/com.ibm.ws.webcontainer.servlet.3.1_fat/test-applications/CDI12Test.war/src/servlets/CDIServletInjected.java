/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi.beans.ConstructorBean;
import cdi.beans.FieldBean;
import cdi.beans.MethodBean;
import cdi.beans.ProducerType;
import cdi.beans.ServletType;

@WebServlet("/CDIServletInjected")
public class CDIServletInjected extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    @ServletType
    private FieldBean fieldBean;

    @Inject
    @ProducerType
    private String producerText;

    private MethodBean methodBean;
    private final ConstructorBean constructorBean;
    private String postConstruct;

    private final String testSubject = ":Servlet:";

    @Inject
    public CDIServletInjected(ConstructorBean bean) {
        constructorBean = bean;
    }

    @PostConstruct
    void start() {
        postConstruct = ":postConstructCalled:";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Inject
    public void setMethodBean(MethodBean bean) {
        methodBean = bean;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();

        if (constructorBean == null) {
            sendResponse(out, ":ConstructorInjectFailed:");
        } else {
            sendResponse(out, constructorBean.getData());
        }

        if (postConstruct == null) {
            sendResponse(out, ":PostConstructFailed:");
        } else {
            sendResponse(out, postConstruct);
        }

        if (fieldBean == null) {
            sendResponse(out, ":FieldInjectFailed:");
        } else {
            sendResponse(out, fieldBean.getData());
        }

        if (producerText == null) {
            sendResponse(out, ":ProducerInjectFailed:");
        } else {
            sendResponse(out, producerText);
        }

        if (methodBean == null) {
            sendResponse(out, ":MethodInjectFailed:");
        } else {
            sendResponse(out, methodBean.getData());
        }

        out.println("Test Exit");

    }

    public void sendResponse(PrintWriter out, String text) {
        out.println(text + testSubject);
    }
}
