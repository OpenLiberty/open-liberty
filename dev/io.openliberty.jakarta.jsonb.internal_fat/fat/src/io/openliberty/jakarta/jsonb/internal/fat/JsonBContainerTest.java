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
package io.openliberty.jakarta.jsonb.internal.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.test.json.b.FakeProvider;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jsonb.container.web.JsonBContainerTestServlet;

@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class JsonBContainerTest extends FATServletClient {

    @Server("io.openliberty.jakarta.jsonb.internal.fat.container")
    @TestServlet(servlet = JsonBContainerTestServlet.class, contextRoot = "jsonbcontainertestapp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        RemoteFile yasson = server.getFileFromLibertySharedDir("resources/yasson/3.0.3/yasson.jar");

        JavaArchive fake_json_b = ShrinkWrap.create(ZipImporter.class, "fake-json-b.jar")
                        .importFrom(new File(yasson.getAbsolutePath()))
                        .as(JavaArchive.class)
                        .addPackage("org.test.json.b")
                        .addAsServiceProvider(jakarta.json.bind.spi.JsonbProvider.class, FakeProvider.class);

        ShrinkHelper.exportToServer(server, "providers", fake_json_b);

        ShrinkHelper.defaultApp(server, "jsonbcontainertestapp", "test.jsonb.container.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
