/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.vistest.standaloneWar.servlet;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.visibility.tests.vistest.commonLib.CommonLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.framework.TestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.overrideLib.OverrideLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.privateLib.PrivateLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtRegular;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtSeeApp;
import com.ibm.ws.cdi.visibility.tests.vistest.standaloneWar.StandaloneWarTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.standaloneWarLib.StandaloneWarLibTestingBean;

import componenttest.app.FATServlet;

/**
 * Accepts requests from the test class to test the visibility of beans from different BDAs
 * <p>
 * We select the TestingBean which resides in the requested BDA, call its doTest method and return the result to the caller.
 */
@WebServlet("/")
public class StandaloneVisibilityTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    // Inject all the testing beans. Each testing bean will check visibility of all the target beans
    // We're using Instance<> here so that if there's a bug which stops one of the testing beans from
    // being visible, the servlet will still start and we can test the visibility from all other points.
    @Inject
    private Instance<CommonLibTestingBean> commonLibTestingInstance;

    @Inject
    private Instance<OverrideLibTestingBean> overrideLibTestingInstance;

    @Inject
    private Instance<PrivateLibTestingBean> privateLibTestingInstance;

    @Inject
    private Instance<StandaloneWarLibTestingBean> standaloneWarLibTestingInstance;

    @Inject
    private Instance<StandaloneWarTestingBean> standaloneWarTestingInstance;

    // Using a qualifier to look this up so that we don't have to have the runtime extension export this package as API
    @Inject
    @InRuntimeExtRegular
    private Instance<TestingBean> runtimeExtRegularTestingInstance;

    // Using a qualifier to look this up so that we don't have to have the runtime extension export this package as API
    @Inject
    @InRuntimeExtSeeApp
    private Instance<TestingBean> runtimeExtSeeAppTestingInstance;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String location = req.getParameter("location");

        String result;
        try {
            if (location == null) {
                result = "ERROR: No qualifier provided\n";
            } else if (location.equals("InCommonLib")) {
                result = commonLibTestingInstance.get().doTest();
            } else if (location.equals("InOverrideLib")) {
                result = overrideLibTestingInstance.get().doTest();
            } else if (location.equals("InPrivateLib")) {
                result = privateLibTestingInstance.get().doTest();
            } else if (location.equals("InRuntimeExtRegular")) {
                result = runtimeExtRegularTestingInstance.get().doTest();
            } else if (location.equals("InRuntimeExtSeeApp")) {
                result = runtimeExtSeeAppTestingInstance.get().doTest();
            } else if (location.equals("InStandaloneWarLib")) {
                result = standaloneWarLibTestingInstance.get().doTest();
            } else if (location.equals("InStandaloneWar")) {
                result = standaloneWarTestingInstance.get().doTest();
            } else {
                result = "ERROR: unrecognised qualifier: " + location + "\n";
            }
        } catch (UnsatisfiedResolutionException ex) {
            result = "ERROR: unable to resolve test class\n" + ex.toString() + "\n";
        }

        resp.getOutputStream().print(result);
    }
}
