/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.config;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi40.internal.fat.config.beansxml.AllBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LegacyConfigTest {
    public static final String SERVER_NAME = "CDI40Server";

    public static final String LEGACY_EMPTY_BEANS_APP_NAME = "LagacyEmptyBeans";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AllBeansServlet.class, contextRoot = LEGACY_EMPTY_BEANS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive legacyEmptyBeansXMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_EMPTY_BEANS_APP_NAME + ".war")
                                                      .addClass(AllBeansServlet.class)
                                                      .addClass(UnannotatedBean.class)
                                                      .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addEmptyBeansXML(legacyEmptyBeansXMLWar);
        ShrinkHelper.exportDropinAppToServer(server, legacyEmptyBeansXMLWar, DeployOptions.SERVER_ONLY);

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXMLExplicitArchive(true);
        cdis.add(cdi);

        server.updateServerConfiguration(config);

        server.startServer(true, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        cdis.clear();
        server.updateServerConfiguration(config);
    }

    /*
     * Testing that the warning is NOT output on CDI 4.0
     */
    @Test
    public void testCdiEmptyBeansXMLExplicitArchiveWarning() throws Exception {
        List<String> warningMessages = server.findStringsInLogs("CWOWB1016W: The attribute cdiEmptyBeansXMLExplicitArchive of element type cdi is only supported on CDI 4.0 and newer. The attribute will be ignored.");
        assertEquals("Message CWOWB1016W was found when it should not have been", 0, warningMessages.size());
    }
}
