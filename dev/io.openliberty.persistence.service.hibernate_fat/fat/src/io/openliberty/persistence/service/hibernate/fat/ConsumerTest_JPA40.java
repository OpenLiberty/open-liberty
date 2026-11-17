/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.persistence.service.hibernate.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.persistence.service.hibernate.fat.web.ConsumerServlet;

/**
 * FAT test for io.openliberty.persistenceService-3.0 backed by Hibernate 8.x / JPA 4.0.
 *
 * Structure mirrors com.ibm.ws.persistence_fat/ConsumerTest_JPA32:
 *  - installSystemFeature installs the test consumer feature manifest
 *  - copyFileToLibertyInstallRoot installs the consumer OSGi bundle (built as a sub-bundle
 *    of this FAT project via io.openliberty.persistence.service.hibernate.consumer.bnd)
 *  - The consumer bundle wires to HibernatePersistenceServiceImpl via persistenceService-3.0
 *  - No JakartaEEAction.transformApp needed — bundle is already EE12/jakarta
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class ConsumerTest_JPA40 extends FATServletClient {

    private static final String APP_NAME = "consumer";

    // Matches IBM-ShortName in the .mf file
    private static final String FEATURE_NAME = "io.openliberty.persistence.service.hibernate.consumer-1.0";
    // Matches Bundle-SymbolicName in the sub-bundle .bnd (jar produced without version suffix)
    private static final String BUNDLE_JAR   = "io.openliberty.persistence.service.hibernate.consumer.jar";

    @Server("io.openliberty.persistence.service.hibernate.consumer.jpa40")
    @TestServlet(servlet = ConsumerServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // 1. Install the test feature manifest
        server.installSystemFeature(FEATURE_NAME);

        // 2. Install the consumer OSGi bundle into Liberty's lib/ directory
        server.copyFileToLibertyInstallRoot("lib/", "bundles/" + BUNDLE_JAR);
        Assert.assertTrue(server.fileExistsInLibertyInstallRoot("lib/" + BUNDLE_JAR));

        // 3. Deploy the WAR containing ConsumerServlet (EE12 — no transform needed)
        ShrinkHelper.defaultDropinApp(server, APP_NAME,
                                      "io.openliberty.persistence.service.hibernate.fat.web");

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CWWKF0001E for transaction-2.1 is a known false positive:
        // transaction-2.1 has visibility:=protected and the FAT log checker
        // cannot see protected features in its public registry lookup.
        server.stopServer("WTRN0074E", "CWWKF0001E");
        server.deleteFileFromLibertyInstallRoot("lib/" + BUNDLE_JAR);
        server.uninstallSystemFeature(FEATURE_NAME);
    }
}
