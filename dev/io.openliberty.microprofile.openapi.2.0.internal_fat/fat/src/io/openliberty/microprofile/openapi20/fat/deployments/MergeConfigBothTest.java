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

import java.util.Arrays;
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
            server.stopServer();
        } finally {
            server.deleteAllDropinApplications();
            server.removeAllInstalledAppsForValidation();
            server.clearAdditionalSystemProperties();
        }
    }

    @Test
    public void testMPConfigIgnoredInServerXMLMode() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        MpOpenAPIElement mpOpenAPI = config.getMpOpenAPIElement();
        mpOpenAPI.getIncludedApplications().clear();
        mpOpenAPI.getIncludedApplications().add("all");
        server.updateServerConfiguration(config);

        //The combo of all and an exclude should be all except testEar/test2, but since this is set via
        //mpConfig this should be ignored.
        setMergeConfig(null, "testEar/test2", null);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModules(war1, war2);

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);

        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        server.startServer();

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test1/test", "/test2/test");
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
        OpenAPITestUtil.checkServersForContextRoot(openapiNode, "");
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
