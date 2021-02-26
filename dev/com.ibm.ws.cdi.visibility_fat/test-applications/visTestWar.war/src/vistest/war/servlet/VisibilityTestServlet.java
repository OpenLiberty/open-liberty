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
package vistest.war.servlet;

import java.io.IOException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;
import vistest.appClientAsEjbLib.AppClientAsEjbLibTestingBean;
import vistest.appClientAsWarLib.AppClientAsWarLibTestingBean;
import vistest.earLib.EarLibTestingBean;
import vistest.ejb.EjbTestingBean;
import vistest.ejbAppClientLib.EjbAppClientLibTestingBean;
import vistest.ejbAsAppClientLib.EjbAsAppClientLibTestingBean;
import vistest.ejbAsEjbLib.EjbAsEjbLibTestingBean;
import vistest.ejbAsWarLib.EjbAsWarLibTestingBean;
import vistest.ejbLib.EjbLibTestingBean;
import vistest.ejbWarLib.EjbWarLibTestingBean;
import vistest.war.WarTestingBean;
import vistest.warAppClientLib.WarAppClientLibTestingBean;
import vistest.warLib.WarLibTestingBean;
import vistest.warWebinfLib.WarWebinfLibTestingBean;

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
            } else {
                result = "ERROR: unrecognised qualifier\n";
            }
        } catch (UnsatisfiedResolutionException ex) {
            result = "ERROR: unable to resolve test class\n" + ex.toString() + "\n";
        }

        resp.getOutputStream().print(result);
    }
}
