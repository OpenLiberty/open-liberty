/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi31.fat.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class OpenAPITckTest {

    private static final String SERVER_NAME = "FATServer";

    public static final FeatureSet MP50_OPENAPI_31 = MicroProfileActions.MP50.removeFeature("mpOpenAPI-3.0")
                                                                             .addFeature("mpOpenAPI-3.1")
                                                                             .build(MicroProfileActions.MP50_ID + "_mpOpenAPI-3.1");

    // Run the TCK with EE9 features and EE10 features
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP60, MP50_OPENAPI_31);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testOpenAPITck() throws Exception {
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getPort(PortType.WC_defaulthost));

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("test.url", protocol + "://" + host + ":" + port);

        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.openapi.3.1.internal_fat_tck", "testOpenAPITck", additionalProps);
        Map<String, String> resultInfo = MvnUtils.getResultInfo(server);
        resultInfo.put("results_type", "MicroProfile");
        resultInfo.put("feature_name", "Open API");
        resultInfo.put("feature_version", "3.1");
        MvnUtils.preparePublicationFile(resultInfo);
    }

}
