/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.shim.OpenTracingShimServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.shim.TracedBean;

/**
 * Test use of the Open Telemetry Autoconfigure Trace SPIs: https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/latest/index.html
 */
@RunWith(FATRunner.class)
public class TelemetryShimTest extends FATServletClient {

    public static final String SHIM_APP_NAME = "shimTest";
    public static final String SERVER_NAME = "Telemetry10Shim";

    @TestServlets({
                    @TestServlet(contextRoot = SHIM_APP_NAME, servlet = OpenTracingShimServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP61_ID)) {
            WebArchive exporterTestWar = ShrinkWrap.create(WebArchive.class, SHIM_APP_NAME + ".war")
                            .addClass(OpenTracingShimServlet.class)
                            .addClass(TracedBean.class)
                            .addAsLibraries(new File("lib/shim129").listFiles());
            ShrinkHelper.exportAppToServer(server, exporterTestWar, SERVER_ONLY);
        } else {
            WebArchive exporterTestWar = ShrinkWrap.create(WebArchive.class, SHIM_APP_NAME + ".war")
                            .addClass(OpenTracingShimServlet.class)
                            .addClass(TracedBean.class)
                            .addAsLibraries(new File("lib/shim").listFiles());
            ShrinkHelper.exportAppToServer(server, exporterTestWar, SERVER_ONLY);
        }

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
