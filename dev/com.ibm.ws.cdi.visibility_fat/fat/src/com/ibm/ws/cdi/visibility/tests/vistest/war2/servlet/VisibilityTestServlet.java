/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest.war2.servlet;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.visibility.tests.vistest.war2.War2TestingBean;

/**
 * Another test servlet which is only used to test visibility from war2 to everywhere else.
 * <p>
 * The majority of testing is done in {@link com.ibm.ws.cdi.visibility.tests.vistest.war.servlet.VisibilityTestServlet}
 */
@WebServlet("/")
public class VisibilityTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private Instance<War2TestingBean> war2TestingInstance;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String qualifier = req.getParameter("location");

        String result;
        try {
            if (qualifier == null) {
                result = "ERROR: No qualifier provided\n";
            } else if (qualifier.equals("InWar2")) {
                result = war2TestingInstance.get().doTest();
            } else {
                result = "ERROR: unrecognised qualifier: " + qualifier + "\n";
            }
        } catch (UnsatisfiedResolutionException ex) {
            result = "ERROR: unable to resolve test class\n" + ex.toString() + "\n";
        }

        resp.getOutputStream().print(result);
    }
}
