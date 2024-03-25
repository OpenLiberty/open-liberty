/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.Cdi;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi40.internal.fat.config.beansxml.AllBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LegacyConfigTest extends FATServletClient {
    public static final String SERVER_NAME = "CDI40LegacyConfigServer";

    public static final String LEGACY_EMPTY_BEANS_APP_NAME = "LegacyEmptyBeans";
    public static final String LEGACY_NON_EMPTY_APP_NAME = "LegacyNonEmpty";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AllBeansServlet.class, contextRoot = LEGACY_EMPTY_BEANS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXmlCDI3Compatibility(true);
        cdis.add(cdi);
        server.updateServerConfiguration(config);

        //this application will only start if the legacy config option has been set (above)
        WebArchive legacyEmptyBeansXMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_EMPTY_BEANS_APP_NAME + ".war")
                                                      .addClass(AllBeansServlet.class)
                                                      .addClass(UnannotatedBean.class)
                                                      .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addEmptyBeansXML(legacyEmptyBeansXMLWar);
        ShrinkHelper.exportDropinAppToServer(server, legacyEmptyBeansXMLWar, DeployOptions.SERVER_ONLY);

        //This application contains a non-empty CDI 4.0 beans.xml. When using CDI 4.0, the discovery mode will default to
        //ANNOTATED. Unlike an empty beans.xml, the legacy configuration option has no effect. Therefore we will output a warning when the
        //DeploymentException occurs
        WebArchive unannotatedlegacyBeans10XMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_NON_EMPTY_APP_NAME + ".war")
                                                              .addClass(AllBeansServlet.class)
                                                              .addClass(UnannotatedBean.class)
                                                              .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addBeansXML(unannotatedlegacyBeans10XMLWar, RequestScopedBean.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, unannotatedlegacyBeans10XMLWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();
    }

    @Test
    public void testUnversionedBeansXmlWarning() throws Exception {
        //check for a warning message about a non-empty unversioned beans.xml file
        List<String> warningMessages = server.findStringsInLogs("CWOWB1018W: .*wsjar:file:.*" + LEGACY_NON_EMPTY_APP_NAME + ".war!/WEB-INF/beans.xml");
        assertTrue("Message CWOWB1018W not found", warningMessages.size() > 0);
        assertEquals("Message CWOWB1018W was found more than once", 1, warningMessages.size());

        //check for the DeploymentException
        List<String> errorMessages = server.findStringsInLogs("CWWKZ0002E:.*" + LEGACY_NON_EMPTY_APP_NAME + ".*DeploymentException");
        assertTrue("Message CWWKZ0002E not found", errorMessages.size() > 0);
        assertEquals("Message CWWKZ0002E was found more than once", 1, errorMessages.size());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWOWB1018W", "CWWKZ0002E");

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        cdis.clear();
        server.updateServerConfiguration(config);
    }
}
