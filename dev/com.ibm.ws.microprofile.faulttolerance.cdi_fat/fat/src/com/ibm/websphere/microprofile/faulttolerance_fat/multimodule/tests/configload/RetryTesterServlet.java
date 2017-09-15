/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("retry-tester")
public class RetryTesterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    RequestRetryCountingBean requestRetryBean;

    @Inject
    AppRetryCountingBean appRetryBean;

    @Inject
    WarRetryCountingBean warRetryBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        try {
            switch (req.getParameter("scope")) {
                case "app":
                    appRetryBean.call();
                    break;
                case "request":
                    requestRetryBean.call();
                    break;
                case "war":
                    warRetryBean.call();
                    break;
                default:
                    throw new RuntimeException("Bad scope parameter");
            }
        } catch (CountingException e) {
            out.println(e.getCount());
        }
    }

}
