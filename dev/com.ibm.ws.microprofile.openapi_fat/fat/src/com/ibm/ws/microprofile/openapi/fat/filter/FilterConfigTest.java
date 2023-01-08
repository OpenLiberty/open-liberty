/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
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
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP60, // mpOpenAPI-3.1, LITE
        MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
        MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
        MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
        MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL

    @BeforeClass
    public static void setUpTest() throws Exception {
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        server.addEnvVar("filter_title", TITLE_VALUE);

        PropertiesAsset config = new PropertiesAsset()
            .addProperty("filter.description", DESC_VALUE)
            .addProperty("mp.openapi.filter", MyTestFilter.class.getName());

        WebArchive war = ShrinkWrap.create(WebArchive.class)
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
        OpenAPI model = OpenAPIConnection.openAPIDocsConnection(server, false).downloadModel();
        Info info = model.getInfo();
        assertEquals(TITLE_VALUE, info.getTitle());
        assertEquals(DESC_VALUE, info.getDescription());
    }

}
