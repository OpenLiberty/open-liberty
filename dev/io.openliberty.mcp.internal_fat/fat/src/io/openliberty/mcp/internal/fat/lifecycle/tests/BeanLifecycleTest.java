/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.lifecycle.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.lifecycle.tools.ClassTool;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

/**
 *
 */
@SuppressWarnings("unused")
@RunWith(FATRunner.class)
public class BeanLifecycleTest {

    @Server("mcp-server")
    public static LibertyServer server;

    public static void info(Class<?> clazz, String method, String message) {
        Log.info(clazz, method, message);
    }

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "beanLifecycleTest.war")
                                   .addPackage(ClassTool.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDependentBeanLifecycle() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.lifecycle.tools.ClassTool",
                            "name": "sayHello",
                            "arguments": {
                              "name": "World"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/beanLifecycleTest", request);
        Log.info(getClass(), "testDependentBeanLifecycle", "Raw MCP response: " + response);

        // Strict Mode tests
        String expectedResponseString = """
                        {
                          "id": "2",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "type": "text", "text": "Hello, World" }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

        assertNotNull(server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool"));
        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        // Print lifecycle messages before asserting
        Log.info(getClass(), "testDependentBeanLifecycle", "Lifecycle messages since mark:");
        lifecycleMessages.forEach(msg -> Log.info(getClass(), "testDependentBeanLifecycle", msg));

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages, contains(containsString("@PostConstruct ClassTool"), containsString("[LOGGED] Class Tool logged"), containsString("@PreDestroy ClassTool")));

    }
}