/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.service.lookup.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.injection.lookup.MyService;

import componenttest.app.FATServlet;

@WebServlet("/ServiceLookupServlet")
public class ServiceLookupServlet extends FATServlet {
    private static final long serialVersionUID = -4674185419088114955L;
    private static final String CLASS_NAME = ServiceLookupServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Test
    public void testOSGiLookup_JDBC() throws Exception {
        testOSGiLookupServlet("java:comp/env/jdbc/Service", false);
    }

    /**
     * This test also ensures that the highest ranking service is returned (with 100)
     */
    @Test
    public void testOSGiLookup_Other() throws Exception {
        testOSGiLookupServlet("java:comp/env/my/Service", false, "ranking of 100");
    }

    @Test
    public void testOSGiLookup_Multi() throws Exception {
        testOSGiLookupServlet("java:comp/env/my/multiPart/Service", false);
    }

    /**
     * This tests that we cannot retrieve a registered OSGi service if there isn't a matching resource entry
     * in the web.xml
     */
    @Test
    public void testOSGiLookup_NoWeb() throws Exception {
        testOSGiLookupServlet("java:comp/env/noWebXML/Service", true);
    }

    @Test
    public void testOSGiLookup_Backslash() throws Exception {
        testOSGiLookupServlet("java:comp/env/my\\backslash\\Service", false);
    }

    private void testOSGiLookupServlet(final String lookupURI, final boolean negativeTest) throws Exception {
        testOSGiLookupServlet(lookupURI, negativeTest, null);
    }

    private void testOSGiLookupServlet(final String lookupURI, final boolean negativeTest, final String extraText) throws Exception {
        String serviceMsg = null;
        boolean exceptionOccurred = false;

        try {
            InitialContext context = new InitialContext();

            final Object service = context.lookup(lookupURI);

            if (service instanceof MyService) {
                svLogger.info("found the service using JNDI lookup!!!");
                serviceMsg = ((MyService) service).run();
                svLogger.info(serviceMsg);
            } else {
                svLogger.info("Service was not compatible!");
                svLogger.info("service=" + service);
            }

        } catch (Exception e) {
            exceptionOccurred = true;
            svLogger.info("lookup failed! Received exception:");
            if (!negativeTest) {
                e.printStackTrace();
            }
            svLogger.info(e.getMessage());
        }

        if (negativeTest) {
            assertTrue("incorrect lookup should have failed.  lookupURI: " + lookupURI, exceptionOccurred);
        } else {
            assertTrue("lookup failed for lookupURI: " + lookupURI, (serviceMsg != null && !exceptionOccurred));
        }

        if (extraText != null) {
            assertTrue("extra text not found: " + extraText, (serviceMsg != null && serviceMsg.contains(extraText)));
        }
    }
}
