/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi40.internal.fat.bce.basicear.lib.EarBCEExtension;
import io.openliberty.cdi40.internal.fat.bce.basicear.lib.LibTestBean;
import io.openliberty.cdi40.internal.fat.bce.basicear.war1.EarBCETestServlet1;
import io.openliberty.cdi40.internal.fat.bce.basicear.war2.EarBCETestServlet2;
import io.openliberty.cdi40.internal.fat.bce.basicwar.BasicBCEExtension;
import io.openliberty.cdi40.internal.fat.bce.basicwar.BasicBCETestServlet;
import io.openliberty.cdi40.internal.fat.bce.beancreatorlookup.BeanCreatorLookupExtension;
import io.openliberty.cdi40.internal.fat.bce.beancreatorlookup.BeanCreatorLookupTestServlet;
import io.openliberty.cdi40.internal.fat.bce.enhance.BceEnhanceTestServlet;
import io.openliberty.cdi40.internal.fat.bce.enhance.EnhanceTestExtension;
import io.openliberty.cdi40.internal.fat.bce.extensionarchivenotscanned.ExtensionArchiveExtension;
import io.openliberty.cdi40.internal.fat.bce.extensionarchivenotscanned.ExtensionArchiveNotScannedTestServlet;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

@RunWith(FATRunner.class)
public class BuildCompatibleExtensionsTest extends FATServletClient {

    public static final String SERVER_NAME = "cdiBceTestServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(contextRoot = "war1", servlet = EarBCETestServlet1.class), //LITE
                    @TestServlet(contextRoot = "war2", servlet = EarBCETestServlet2.class), //LITE
                    @TestServlet(contextRoot = "basicWar", servlet = BasicBCETestServlet.class), //LITE
                    @TestServlet(contextRoot = "enhance", servlet = BceEnhanceTestServlet.class), //FULL
                    @TestServlet(contextRoot = "extensionArchiveNotScanned", servlet = ExtensionArchiveNotScannedTestServlet.class), //FULL
                    @TestServlet(contextRoot = "beanCreatorLookup", servlet = BeanCreatorLookupTestServlet.class) //FULL
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // basicwar.war
        // ------------
        Package warPackage = BasicBCETestServlet.class.getPackage();
        WebArchive basicWar = ShrinkWrap.create(WebArchive.class, "basicWar.war")
                                        .addPackage(warPackage)
                                        .addAsServiceProvider(BuildCompatibleExtension.class, BasicBCEExtension.class)
                                        .addAsWebInfResource(warPackage, "beans.xml", "beans.xml")
                                        .addAsManifestResource(warPackage, "permissions.xml", "permissions.xml"); // Workaround WELD-2705

        ShrinkHelper.exportDropinAppToServer(server, basicWar, DeployOptions.SERVER_ONLY);

        // basicear.ear
        // --------------
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                                    .addPackage(EarBCETestServlet1.class.getPackage());

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                                    .addPackage(EarBCETestServlet2.class.getPackage());

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar")
                                    .addPackage(LibTestBean.class.getPackage())
                                    .addAsServiceProvider(BuildCompatibleExtension.class, EarBCEExtension.class)
                                    .addAsManifestResource(LibTestBean.class.getPackage(), "beans.xml", "beans.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "basicear.ear")
                                          .addAsModules(war1, war2)
                                          .addAsLibraries(lib)
                                          .addAsManifestResource(LibTestBean.class.getPackage(), "earPermissions.xml", "permissions.xml"); // Workaround WELD-2705

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        // enhance.war
        // -----------
        Package enhancePackage = BceEnhanceTestServlet.class.getPackage();
        WebArchive enhanceWar = ShrinkWrap.create(WebArchive.class, "enhance.war")
                                          .addPackage(enhancePackage)
                                          .addAsServiceProvider(BuildCompatibleExtension.class, EnhanceTestExtension.class)
                                          .addAsWebInfResource(enhancePackage, "beans.xml", "beans.xml")
                                          .addAsManifestResource(enhancePackage, "permissions.xml", "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, enhanceWar, DeployOptions.SERVER_ONLY);

        // extensionArchiveNotScanned.war
        // ------------------------------
        Package archiveNotScannedPackage = ExtensionArchiveNotScannedTestServlet.class.getPackage();
        WebArchive archiveNotScannedWar = ShrinkWrap.create(WebArchive.class, "extensionArchiveNotScanned.war")
                                                    .addPackage(archiveNotScannedPackage)
                                                    .addAsServiceProvider(BuildCompatibleExtension.class, ExtensionArchiveExtension.class);

        ShrinkHelper.exportDropinAppToServer(server, archiveNotScannedWar, DeployOptions.SERVER_ONLY);

        // beanCreatorLookup.war
        // ---------------------
        Package beanCreatorLookupPackage = BeanCreatorLookupTestServlet.class.getPackage();
        WebArchive beanCreatorLookupWar = ShrinkWrap.create(WebArchive.class, "beanCreatorLookup.war")
                                                    .addPackage(beanCreatorLookupPackage)
                                                    .addAsServiceProvider(BuildCompatibleExtension.class, BeanCreatorLookupExtension.class)
                                                    .addAsWebInfResource(beanCreatorLookupPackage, "beans.xml", "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, beanCreatorLookupWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
