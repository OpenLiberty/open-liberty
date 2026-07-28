/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.unsupportedAnnotationApp.UnsupportedAnnotationTools;

/**
 * Verifies that methods annotated with unsupported MCP annotations (@Prompt, @Resource,
 * @ResourceTemplate, @CompletePrompt, @CompleteResourceTemplate) produce a CWMCM0043W
 * warning, while the application still starts and the valid @Tool method remains usable.
 */
@RunWith(FATRunner.class)
public class UnsupportedAnnotationWarningTest extends FATServletClient {

    private static final String APP_NAME = "unsupportedAnnotationTest";
    private static final String BEAN_CLASS = UnsupportedAnnotationTools.class.getName();

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(UnsupportedAnnotationTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMCM0043W");
    }

    @Test
    public void testWarnOnPromptAnnotation() throws Exception {
        String expected = Pattern.quote("CWMCM0043W: The " + BEAN_CLASS + ".promptMethod method uses the @Prompt annotation which is not supported. This method is ignored.");
        assertNotNull("Expected CWMCM0043W for @Prompt not found in logs", server.waitForStringInLog(expected));
    }

    @Test
    public void testWarnOnResourceAnnotation() throws Exception {
        String expected = Pattern.quote("CWMCM0043W: The " + BEAN_CLASS + ".resourceMethod method uses the @Resource annotation which is not supported. This method is ignored.");
        assertNotNull("Expected CWMCM0043W for @Resource not found in logs", server.waitForStringInLog(expected));
    }

    @Test
    public void testWarnOnResourceTemplateAnnotation() throws Exception {
        String expected = Pattern.quote("CWMCM0043W: The " + BEAN_CLASS
                                        + ".resourceTemplateMethod method uses the @ResourceTemplate annotation which is not supported. This method is ignored.");
        assertNotNull("Expected CWMCM0043W for @ResourceTemplate not found in logs", server.waitForStringInLog(expected));
    }

    @Test
    public void testWarnOnCompletePromptAnnotation() throws Exception {
        String expected = Pattern.quote("CWMCM0043W: The " + BEAN_CLASS
                                        + ".completePromptMethod method uses the @CompletePrompt annotation which is not supported. This method is ignored.");
        assertNotNull("Expected CWMCM0043W for @CompletePrompt not found in logs", server.waitForStringInLog(expected));
    }

    @Test
    public void testWarnOnCompleteResourceTemplateAnnotation() throws Exception {
        String expected = Pattern.quote("CWMCM0043W: The " + BEAN_CLASS
                                        + ".completeResourceTemplateMethod method uses the @CompleteResourceTemplate annotation which is not supported. This method is ignored.");
        assertNotNull("Expected CWMCM0043W for @CompleteResourceTemplate not found in logs", server.waitForStringInLog(expected));
    }
}
