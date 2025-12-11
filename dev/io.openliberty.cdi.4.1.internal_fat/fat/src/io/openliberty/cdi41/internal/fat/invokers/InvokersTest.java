/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.invokers;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi41.internal.fat.invokers.app.InvokersExtension;
import io.openliberty.cdi41.internal.fat.invokers.app.InvokersTestServlet;
import jakarta.enterprise.inject.spi.Extension;

/**
 * CDI TCK tests correct invoker behavior.
 *
 * This test mostly covers the servicability when invokers are used incorrectly.
 */
@RunWith(FATRunner.class)
public class InvokersTest {

    public static final String APP_NAME = "invokersTest";

    @TestServlet(contextRoot = APP_NAME, servlet = InvokersTestServlet.class)
    @Server("cdi41Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(InvokersTestServlet.class.getPackage())
                                   .addAsServiceProvider(Extension.class, InvokersExtension.class);
        CDIArchiveHelper.addBeansXML(war, DiscoveryMode.ANNOTATED);

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    public static void teardown() throws Exception {
        server.stopServer();
    }
}
