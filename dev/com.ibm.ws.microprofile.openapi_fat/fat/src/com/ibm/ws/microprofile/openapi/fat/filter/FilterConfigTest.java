/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.fat.filter;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.FATSuite;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Checks that a filter can be defined and called and can use values from MP
 * Config
 */
@RunWith(FATRunner.class)
public class FilterConfigTest {

    private static final String TITLE_VALUE = "title from config";
    private static final String DESC_VALUE = "description from config";

    private static final String SERVER_NAME = "FilterServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUpTest() throws Exception {
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        server.addEnvVar("filter_title", TITLE_VALUE);

        PropertiesAsset config = new PropertiesAsset()
            .addProperty("filter.description", DESC_VALUE)
            .addProperty("mp.openapi.filter", MyTestFilter.class.getName());

        WebArchive war = ShrinkWrap.create(WebArchive.class, "filter-test.war")
            .addClasses(FilterTestApp.class, FilterTestResource.class, MyTestFilter.class)
            .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testFilterConfig() throws Exception {
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode model = OpenAPITestUtil.readYamlTree(doc);

        OpenAPITestUtil.checkInfo(model, TITLE_VALUE, "1.0", DESC_VALUE);
    }

}
