/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.xmlbinding40.fat;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.xmlbinding40.fat.util.ExplodedShrinkHelper;
import jaxb.web.JAXBContextTestServlet;

/**
 * This test is intended to use the JAXBContext object to marshall and unmarshall various Java types on the Liberty runtime
 * and test the ability of the runtime to handle more than one JAXB Implementation if Configured.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class LibertyJAXBRIContextTest extends FATServletClient {

    private static final String APP_NAME = "jaxbApp";

    @Server("jaxb_fat")
    @TestServlet(servlet = JAXBContextTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ExplodedShrinkHelper.explodedDropinApp(server, APP_NAME, "jaxb.web", "jaxb.web.dataobjects", "jaxb.web.utils");

        server.startServer();

        assertNotNull("The jaxb_fat server did not start", server.waitForStringInLog("CWWKF0011I"));
    }

    @Test
    public void testExistanceOfThirdPartyJarFiles() throws Exception {
        Class<?> c = LibertyJAXBRIContextTest.class;
        // Continue logging files in the folder !!

        List<String> webINFFileNames = server.listLibertyServerRoot("dropins/jaxbApp.war/WEB-INF", null);
        List<String> autoFVTFileNames = server.listAutoFVTTestFiles(server.getMachine(), "./../../test-applications/thirdPartyJaxbImplContextApp/resources/WEB-INF/lib", null);

        if (webINFFileNames.isEmpty())
            Log.info(c, "testThirdpartyJarFiles", "dropins/jaxbApp.war/WEB-INF isEmpty");

        for (String fn : webINFFileNames) {
            Log.info(c, "testThirdpartyJarFiles", "List of WEB-INF files/folders: " + fn);
        }

        if (autoFVTFileNames.isEmpty())
            Log.info(c, "testThirdpartyJarFiles", "autoFVT/test-applications/thirdPartyJaxbImplContextApp/resources/WEB-INF/lib isEmpty");

        for (String fn : autoFVTFileNames) {
            Log.info(c, "testThirdpartyJarFiles", "List of autoFVT/test-applications/thirdPartyJaxbImplContextApp/resources/WEB-INF/lib files: " + fn);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKW1405W", "CWWKW1404W");
        }
    }

}