/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakarta.restfulWS30api.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jakarta.restfulWS30api.fat.app.canload.ApiTestServlet;

@RunWith(FATRunner.class)
public class CanLoadRESTfulWS30APIsTest extends FATServletClient {
    static String[] ee9Array = {"restfulWSClient-3.0","xmlBinding-3.0"};
    static Set<String> ee9Set = new HashSet<String>(Arrays.asList(ee9Array));
    static String[] ee10Array = {"restfulWS-3.1","xmlBinding-4.0"};
    static Set<String> ee10Set = new HashSet<String>(Arrays.asList(ee10Array));
    
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("restfulWS-3.0", "restfulWSClient-3.0").withID("ClientFeature"))
                    .andWith(new FeatureReplacementAction(ee9Set, ee10Set).withID("EE10"))
                    .andWith(new FeatureReplacementAction("restfulWS-3.1", "restfulWSClient-3.1").withID("EE10-ClientFeature"));

                    
    public static final String APP_NAME = "jaxrs30api";
    public static final String SERVER_NAME = "jaxrs30api";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ApiTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, ApiTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
