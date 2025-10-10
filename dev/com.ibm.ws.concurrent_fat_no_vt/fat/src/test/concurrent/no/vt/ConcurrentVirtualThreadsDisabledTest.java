/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.concurrent.no.vt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import test.concurrent.no.vt.web.ConcurrentVTDisabledServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConcurrentVirtualThreadsDisabledTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.no.vt")
    @TestServlet(servlet = ConcurrentVTDisabledServlet.class,
                 contextRoot = "ConcurrentNoVTWeb")
    public static LibertyServer server;

    /**
     * Asserts that a String within the list contains the substring.
     *
     * @param substring text to search for.
     * @param list      strings to search.
     */
    private static void assertContains(String substring, List<String> list) {
        boolean found = false;
        for (String item : list)
            if (item.contains(substring)) {
                found = true;
                break;
            }

        if (!found)
            fail("Expected item with substring " + substring + " in " + list);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ConcurrentNoVTWeb = ShrinkHelper
                        .buildDefaultApp("ConcurrentNoVTWeb",
                                         "test.concurrent.no.vt.web");
        ShrinkHelper.addDirectory(ConcurrentNoVTWeb,
                                  "test-applications/ConcurrentNoVTWeb/resources");
        ShrinkHelper.exportAppToServer(server, ConcurrentNoVTWeb);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (Runtime.version().feature() >= 21) {
                List<String> mtfMessages = server.findStringsInLogs("CWWKC1108I");

                assertContains("concurrent/ServerXMLThreadFactoryToOverride",
                               mtfMessages);
                assertContains("java:comp/concurrent/WebXMLThreadFactoryToOverride",
                               mtfMessages);
                assertContains("java:module/concurrent/AnnoThreadFactoryToOverride",
                               mtfMessages);

                // TODO why are multiple ManagedThreadFactoryService being created for the same configuration element?
                //assertEquals(mtfMessages.toString(), 3, mtfMessages.size());

                List<String> policyMessages = server.findStringsInLogs("CWWKE1208I");

                assertContains("application[ConcurrentNoVTWeb]/managedScheduledExecutorService[java:app/concurrent/AnnoScheduledExecutorToOverride]/concurrencyPolicy",
                               policyMessages);
                assertContains("application[ConcurrentNoVTWeb]/module[ConcurrentNoVTWeb.war]/managedExecutorService[java:comp/concurrent/AnnoExecutorToOverride]/concurrencyPolicy",
                               policyMessages);
                assertContains("application[ConcurrentNoVTWeb]/module[ConcurrentNoVTWeb.war]/managedScheduledExecutorService[java:module/concurrent/WebXMLScheduledExecutorToOverride]/concurrencyPolicy",
                               policyMessages);
                assertContains("managedExecutorService[executorToOverride]/concurrencyPolicy[default-0]",
                               policyMessages);
                assertContains("managedExecutorService[java:global/concurrent/WebXMLExecutorToOverride]/concurrencyPolicy",
                               policyMessages);
                assertContains("virtualThreadPolicyToOverride",
                               policyMessages);

                assertEquals(policyMessages.toString(), 6, policyMessages.size());
            }
        } finally {
            server.stopServer();
        }
    }
}
