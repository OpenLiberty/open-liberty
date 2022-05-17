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
import static org.junit.Assert.assertTrue;

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
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;
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
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedErrorServletBeans10;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedErrorServletEmptyBeansXml;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LegacyConfigTestError extends FATServletClient {
    public static final String SERVER_NAME = "CDI40Server";

    public static final String LEGACY_EMPTY_BEANS_APP_NAME = "UnannotatedErrorServletEmptyBeansXml";
    public static final String LEGACY_BEANS10_APP_NAME = "UnannotatedErrorServletBeans10";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = UnannotatedErrorServletEmptyBeansXml.class, contextRoot = LEGACY_EMPTY_BEANS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXMLExplicitBeanArchive(true);
        cdis.add(cdi);

        server.updateServerConfiguration(config);

        //an empty beans.xml file should not produce a warning even though this app will result in a DeploymentException
        WebArchive legacyEmptyBeansXMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_EMPTY_BEANS_APP_NAME + ".war")
                                                      .addClass(UnannotatedErrorServletEmptyBeansXml.class)
                                                      .addClass(UnannotatedBean.class)
                                                      .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addEmptyBeansXML(legacyEmptyBeansXMLWar);
        ShrinkHelper.exportDropinAppToServer(server, legacyEmptyBeansXMLWar, DeployOptions.SERVER_ONLY);

        //This legacyBeans10XMLWar war contains a non-empty CDI 1.0 beans.xml. When using CDI 4.0, the discovery mode will default to
        //ANNOTATED. Unlike an empty beans.xml, the legacy configuration option has no effect. Therefore we will output a warning when the
        //DeploymentException occurs
        WebArchive legacyBeans10XMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_BEANS10_APP_NAME + ".war")
                                                   .addClass(UnannotatedErrorServletBeans10.class)
                                                   .addClass(UnannotatedBean.class)
                                                   .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addBeansXML(legacyBeans10XMLWar, CDIVersion.CDI10);
        ShrinkHelper.exportDropinAppToServer(server, legacyBeans10XMLWar, DeployOptions.SERVER_ONLY);

        server.startServer(true, false);
    }

    @Test
    public void testUnversionedBeansXmlWarning() throws Exception {
        List<String> warningMessages = server.findStringsInLogs("CWOWB1018W:.*");
        assertTrue("Message CWOWB1018W not found", warningMessages.size() > 0);
        assertEquals("Message CWOWB1018W was found more than once", 1, warningMessages.size());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWOWB1018W", "CWWKZ0002E");
    }
}
