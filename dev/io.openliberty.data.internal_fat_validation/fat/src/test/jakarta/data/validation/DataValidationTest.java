/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
package test.jakarta.data.validation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.validation.web.DataValidationTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataValidationTest extends FATServletClient {

    private static final String APP_NAME = "DataValidationTestApp";

    @Server("io.openliberty.data.internal.fat.validation")
    @TestServlet(servlet = DataValidationTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("DataValidationTestApp", "test.jakarta.data.validation.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Verify that data logValues config limits the Entitlements repository
        // to only logging customer data on its findById method,
        // and that it does not log customer data from its save method.
        boolean found4 = false;
        boolean found5 = false;
        List<String> unexpected = new ArrayList<>();
        try {
            for (String line : server.findStringsInLogsAndTrace("Entitlement#")) {
                if (line.contains(" eclipselink.ps."))
                    ; // EclipseLink logging, not ours
                else if (line.contains("Entitlement#4:US-SNAP AS_NEEDED for person4@openliberty.io"))
                    found4 = true; // returned by findById which is allowed to log data
                else if (line.contains("Entitlement#5:US-TANF MONTHLY for person5@openliberty.io from age 13"))
                    found5 = true; // returned by findById which is allowed to log data
                else
                    unexpected.add(line);
            }
        } finally {
            server.stopServer();
        }

        assertEquals("Found " + unexpected.size() +
                     " unexpected lines of customer data (containing ...Entitlement#...) in logs.",
                     0, unexpected.size());
        assertEquals(true, found4);
        assertEquals(true, found5);
    }
}
