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
package com.ibm.ws.cdi.visibility.tests.vistest.war.servlet;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsEjbLib.AppClientAsEjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsWarLib.AppClientAsWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.commonLib.CommonLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.earLib.EarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejb.EjbTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAppClientLib.EjbAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsAppClientLib.EjbAsAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsEjbLib.EjbAsEjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsWarLib.EjbAsWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbLib.EjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbWarLib.EjbWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.framework.TestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.overrideLib.OverrideLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.privateLib.PrivateLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtRegular;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtSeeApp;
import com.ibm.ws.cdi.visibility.tests.vistest.war.WarTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warAppClientLib.WarAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warLib.WarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warWebinfLib.WarWebinfLibTestingBean;

import componenttest.app.FATServlet;

/**
 * Accepts requests from the test class to test the visibility of beans from different BDAs
 * <p>
 * We select the TestingBean which resides in the requested BDA, call its doTest method and return the result to the caller.
 */
@WebServlet("/")
public class VisibilityTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    // Inject all the testing beans. Each testing bean will check visibility of all the target beans
    // We're using Instance<> here so that if there's a bug which stops one of the testing beans from
    // being visible, the servlet will still start and we can test the visibility from all other points.
    @Inject
    private Instance<WarTestingBean> warTestingInstance;

    @Inject
    private Instance<EjbTestingBean> ejbTestingInstance;

    @Inject
    private Instance<WarLibTestingBean> warLibTestingInstance;

    @Inject
    private Instance<WarWebinfLibTestingBean> warWebinfLibTestingInstance;

    @Inject
    private Instance<EjbLibTestingBean> ejbLibTestingInstance;

    @Inject
    private Instance<EjbWarLibTestingBean> ejbWarLibTestingInstance;

    @Inject
    private Instance<WarAppClientLibTestingBean> warAppClientLibTestingInstance;

    @Inject
    private Instance<EjbAppClientLibTestingBean> ejbAppClientLibTestingInstance;

    @Inject
    private Instance<EarLibTestingBean> earLibTestingInstance;

    @Inject
    private Instance<EjbAsEjbLibTestingBean> ejbAsEjbLibTestingInstance;

    @Inject
    private Instance<EjbAsWarLibTestingBean> ejbAsWarLibTestingInstance;

    @Inject
    private Instance<EjbAsAppClientLibTestingBean> ejbAsAppClientLibTestingInstance;

    @Inject
    private Instance<AppClientAsEjbLibTestingBean> appClientAsEjbLibTestingInstance;

    @Inject
    private Instance<AppClientAsWarLibTestingBean> appClientAsWarLibTestingInstance;

    @Inject
    private Instance<CommonLibTestingBean> commonLibTestingInstance;

    @Inject
    private Instance<PrivateLibTestingBean> privateLibTestingInstance;

    @Inject
    private Instance<OverrideLibTestingBean> overrideLibTestingInstance;

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
            } else if (location.equals("InWar")) {
                result = warTestingInstance.get().doTest();
            } else if (location.equals("InEjb")) {
                result = ejbTestingInstance.get().doTest();
            } else if (location.equals("InWarLib")) {
                result = warLibTestingInstance.get().doTest();
            } else if (location.equals("InWarWebinfLib")) {
                result = warWebinfLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbLib")) {
                result = ejbLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbWarLib")) {
                result = ejbWarLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbAppClientLib")) {
                result = ejbAppClientLibTestingInstance.get().doTest();
            } else if (location.equals("InWarAppClientLib")) {
                result = warAppClientLibTestingInstance.get().doTest();
            } else if (location.equals("InEarLib")) {
                result = earLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbAsEjbLib")) {
                result = ejbAsEjbLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbAsWarLib")) {
                result = ejbAsWarLibTestingInstance.get().doTest();
            } else if (location.equals("InEjbAsAppClientLib")) {
                result = ejbAsAppClientLibTestingInstance.get().doTest();
            } else if (location.equals("InAppClientAsEjbLib")) {
                result = appClientAsEjbLibTestingInstance.get().doTest();
            } else if (location.equals("InAppClientAsWarLib")) {
                result = appClientAsWarLibTestingInstance.get().doTest();
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
            } else {
                result = "ERROR: unrecognised qualifier: " + location + "\n";
            }
        } catch (UnsatisfiedResolutionException ex) {
            result = "ERROR: unable to resolve test class\n" + ex.toString() + "\n";
        }

        resp.getOutputStream().print(result);
    }
}
