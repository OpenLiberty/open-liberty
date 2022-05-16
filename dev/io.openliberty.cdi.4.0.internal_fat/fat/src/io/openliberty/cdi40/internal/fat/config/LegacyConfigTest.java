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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import io.openliberty.cdi40.internal.fat.config.beansxml.AllBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.AnnotatedBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LegacyConfigTest extends FATServletClient {
    public static final String SERVER_NAME = "CDI40Server";

    public static final String LEGACY_EMPTY_BEANS_APP_NAME = "LagacyEmptyBeans";
    public static final String LEGACY_BEANS10_APP_NAME = "LagacyBeans10";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AllBeansServlet.class, contextRoot = LEGACY_EMPTY_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = LEGACY_BEANS10_APP_NAME)
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

        //This legacyBeans10XMLWar war contains a non-empty CDI 1.0 beans.xml. This means that when using CDI 4.0, it will default to a discovery mode of
        //ANNOTATED. The legacy configuration option has no effect. At first we thought that it should have been treated the same an empty
        //beans.xml such that the config option would have maintained legacy behaviour ... but this is not so and is consistent with the
        //spec (which only mentions empty beans.xml files).
        WebArchive legacyBeans10XMLWar = ShrinkWrap.create(WebArchive.class, LEGACY_BEANS10_APP_NAME + ".war")
                                                   .addClass(AnnotatedBeansServlet.class)
                                                   .addClass(UnannotatedBean.class)
                                                   .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addBeansXML(legacyBeans10XMLWar, CDIVersion.CDI10);
        ShrinkHelper.exportDropinAppToServer(server, legacyBeans10XMLWar, DeployOptions.SERVER_ONLY);

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXMLExplicitBeanArchive(true);
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
}
