/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jdbc.h2.web.H2TestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class H2Test extends FATServletClient {

    @Server("com.ibm.ws.jdbc.fat.h2")
    @TestServlet(servlet = H2TestServlet.class, contextRoot = "H2TestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("H2TestApp",
                                                      "test.jdbc.h2.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        
        /**
         * Test that passwords are not leaked in H2 logwriter output.
         * The H2 logwriter is enabled via bootstrap.properties with:
         * com.ibm.ws.h2.logwriter=all
         *
         * This test verifies that the password "dbpwd1" used in the
         * DataSourceDefinition is not leaked in the trace logs.
         */

        // Explicitly verify that the password "dbpwd1" is not in trace.log
        List<String> passwordsInTrace = server.findStringsInLogsAndTrace("dbpwd1");
        assertEquals("Password 'dbpwd1' should not appear in trace.log",
                     0, passwordsInTrace.size());

        // Verify that H2 logwriter output exists (shows trace is enabled)
        List<String> h2LogWriterOutput = server.findStringsInLogsAndTrace("com\\.ibm\\.ws\\.h2\\.logwriter");
        assertTrue("H2 logwriter output should be present in trace.log",
                   !h2LogWriterOutput.isEmpty());

        // Verify exact Type:/Content: password filtering behavior
        List<String> passwordTypeLines = server.findStringsInLogsAndTrace("Type: password");
        assertTrue("Type: password should be present in trace.log",
                   !passwordTypeLines.isEmpty());

        List<String> filteredContent = server.findStringsInLogsAndTrace("Content: \\*\\*\\*\\*\\*\\*");
        assertTrue("Filtered content markers (Content: ******) should be present in trace.log",
                   !filteredContent.isEmpty());

        // Verify that all Content: lines are filtered (no unfiltered content should appear)
        List<String> unfilteredContent = server.findStringsInLogsAndTrace("Content: (?!\\*\\*\\*\\*\\*\\*).*");
        assertEquals("All Content: lines should be filtered in trace.log",
                     0, unfilteredContent.size());

        server.stopServer();
    }

}
