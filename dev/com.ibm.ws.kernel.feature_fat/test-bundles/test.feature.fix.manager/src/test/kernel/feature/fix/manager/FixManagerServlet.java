/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.kernel.feature.fix.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.kernel.feature.FixManager;

import componenttest.app.FATServlet;

@WebServlet("/fixManagerServlet")
public class FixManagerServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public void emptyFixList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
        if (fixManagerRef != null) {
            fixManagerService = ctxt.getService(fixManagerRef);
        }
        if (fixManagerService != null) {
            assertEquals("getIFix list should return: ", 0, fixManagerService.getIFixes().size());
            assertEquals("getTFix list should return: ", 0, fixManagerService.getTFixes().size());
        }
    }

    public void singleIFix(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
        if (fixManagerRef != null) {
            fixManagerService = ctxt.getService(fixManagerRef);
        }
        if (fixManagerService != null) {
            assertThat(fixManagerService.getIFixes(), Matchers.hasItem("APAR0007"));
        }
    }

    public void multiIFixes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
        if (fixManagerRef != null) {
            fixManagerService = ctxt.getService(fixManagerRef);
        }
        if (fixManagerService != null) {
            assertThat(fixManagerService.getIFixes(), Matchers.hasItems("APAR0005", "APAR0006", "APAR0007", "APAR0008"));
        }
    }

    public void singleTFix(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
        if (fixManagerRef != null) {
            fixManagerService = ctxt.getService(fixManagerRef);
        }
        if (fixManagerService != null) {
            assertThat(fixManagerService.getTFixes(), Matchers.hasItem("TestAPAR0001"));
        }
    }

    public void multiTFixes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
        if (fixManagerRef != null) {
            fixManagerService = ctxt.getService(fixManagerRef);
        }
        if (fixManagerService != null) {
            assertThat(fixManagerService.getTFixes(), Matchers.hasItems("TestAPAR0001", "TestAPAR0002"));
        }
    }

}
