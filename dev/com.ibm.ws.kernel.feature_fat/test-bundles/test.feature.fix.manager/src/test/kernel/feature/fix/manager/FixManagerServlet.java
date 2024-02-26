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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.kernel.feature.FixManager;

import componenttest.app.FATServlet;

@WebServlet("/fixManagerServlet")
public class FixManagerServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public void emptyFixList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
            if (fixManagerRef != null) {
                fixManagerService = ctxt.getService(fixManagerRef);
            }
            if (fixManagerService != null) {
                assertEquals("getIFix list should return: ", 0, fixManagerService.getIFixes().size());
                assertEquals("getTFix list should return: ", 0, fixManagerService.getTFixes().size());
            }
        } finally {
        }
    }

    public void singleIFix(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
            if (fixManagerRef != null) {
                fixManagerService = ctxt.getService(fixManagerRef);
            }
            if (fixManagerService != null) {
                assertEquals("getIFix list should return: ", 1, fixManagerService.getIFixes().size());
                assertThat(fixManagerService.getIFixes(), containsInAnyOrder("APAR0007"));
            }
        } finally {
        }
    }

    public void multiIFixes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
            if (fixManagerRef != null) {
                fixManagerService = ctxt.getService(fixManagerRef);
            }
            if (fixManagerService != null) {
                assertEquals("getIFix list should return: ", 4, fixManagerService.getIFixes().size());
                assertThat(fixManagerService.getIFixes(), containsInAnyOrder("APAR0005", "APAR0006", "APAR0007", "APAR0008"));
            }
        } finally {
        }
    }

    public void singleTFix(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
            if (fixManagerRef != null) {
                fixManagerService = ctxt.getService(fixManagerRef);
            }
            if (fixManagerService != null) {
                assertEquals("getTFix list should return: ", 1, fixManagerService.getTFixes().size());
                assertThat(fixManagerService.getTFixes(), containsInAnyOrder("TestAPAR0001"));
            }
        } finally {
        }
    }

    public void multiTFixes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FixManager fixManagerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FixManager> fixManagerRef = ctxt.getServiceReference(FixManager.class);
            if (fixManagerRef != null) {
                fixManagerService = ctxt.getService(fixManagerRef);
            }
            if (fixManagerService != null) {
                System.out.println("SIZEEE: " + fixManagerService.getTFixes());
                assertEquals("getTFix list should return: ", 2, fixManagerService.getTFixes().size());
                assertThat(fixManagerService.getTFixes(), containsInAnyOrder("TestAPAR0001", "TestAPAR0002"));
            }
        } finally {
        }
    }

}
