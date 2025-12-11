/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.MpOpenAPIInfoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Test setting MP Config and server.xml merge config at the same time
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MergeConfigBothTest {

    private static final String SERVER_NAME = "OpenAPIMergeWithServerXMLTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatReduced(SERVER_NAME);

    @After
    public void cleanup() throws Exception {
        try {
            server.stopServer("CWWKO1686W");
        } finally {
            server.deleteAllDropinApplications();
            server.removeAllInstalledAppsForValidation();
            server.clearAdditionalSystemProperties();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMPConfigIgnoredInServerXMLMode() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        MpOpenAPIElement mpOpenAPI = config.getMpOpenAPIElement();
        mpOpenAPI.getIncludedApplications().clear();
        mpOpenAPI.getIncludedApplications().add("all");
        MpOpenAPIInfoElement info = new MpOpenAPIInfoElement();
        info.setTitle("Test App ServerXml");
        info.setVersion("1.0");
        mpOpenAPI.setInfo(info);
        server.updateServerConfiguration(config);

        //The combo of all and an exclude should be all except testEar/test2, but since this is set via
        //mpConfig this should be ignored.
        setMergeConfig(null, "testEar/test2", "{\"title\": \"Test App\", \"version\":\"1.0\"}");

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2);

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);

        server.startServer();

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Test App ServerXml", "1.0");
        OpenAPITestUtil.checkServersForContextRoot(openapiNode, "");

        // Expect warning that MP Config ignored because it conflicts
        assertThat(server.findStringsInLogs("CWWKO1686W"),
                   containsInAnyOrder(containsString("mp.openapi.extensions.liberty.merged.exclude"),
                                      containsString("mp.openapi.extensions.liberty.merged.info")));
        assertThat(server.findStringsInLogs("CWWKO1685I"), empty());

        // Now update the server.xml merging config so that it's equal to the MP Config config
        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        mpOpenAPI = config.getMpOpenAPIElement();
        mpOpenAPI.getIncludedApplications().clear();
        mpOpenAPI.getExcludedModules().add("testEar/test2");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Expect info message for merge config that MP Config ignored because it is equivalent
        assertThat(server.findStringsInLogsUsingMark("CWWKO1685I", server.getDefaultLogFile()),
                   contains(containsString("mp.openapi.extensions.liberty.merged.exclude")));
        // No message expected for info config since it hasn't changed
        assertThat(server.findStringsInLogsUsingMark("CWWKO1686W", server.getDefaultLogFile()), empty());

        // Now update the server.xml info config so that it's equal to the MP Config config
        server.setMarkToEndOfLog();
        config = server.getServerConfiguration();
        mpOpenAPI = config.getMpOpenAPIElement();
        info = mpOpenAPI.getInfo();
        info.setTitle("Test App");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Expect info message for info config that MP Config ignored because it is equivalent
        // No message expected for merge config because it hasn't changed
        assertThat(server.findStringsInLogsUsingMark("CWWKO1685I", server.getDefaultLogFile()),
                   contains(containsString("mp.openapi.extensions.liberty.merged.info")));
        assertThat(server.findStringsInLogsUsingMark("CWWKO1686W", server.getDefaultLogFile()), empty());
    }

    private void setMergeConfig(String included,
                                String excluded,
                                String info) {
        Map<String, String> configProps = new HashMap<>();
        if (included != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_include", included);
        }

        if (excluded != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_exclude", excluded);
        }

        if (info != null) {
            configProps.put("mp_openapi_extensions_liberty_merged_info", info);
        }

        server.setAdditionalSystemProperties(configProps);
    }

}
