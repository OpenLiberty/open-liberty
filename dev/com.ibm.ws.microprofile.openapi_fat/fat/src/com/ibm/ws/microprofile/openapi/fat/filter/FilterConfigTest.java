/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Checks that a filter can be defined and called and can use values from MP
 * Config
 */
@RunWith(FATRunner.class)
public class FilterConfigTest {

    private static final String TITLE_VALUE = "title from config";
    private static final String DESC_VALUE = "description from config";

    @Server("FilterServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();
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
