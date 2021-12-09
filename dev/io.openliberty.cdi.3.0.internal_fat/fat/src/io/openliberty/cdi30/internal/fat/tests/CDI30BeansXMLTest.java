/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi30.internal.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi30.internal.fat.apps.beansxml.AnnotatedBean;
import io.openliberty.cdi30.internal.fat.apps.beansxml.CDI30BeansXMLTestServlet;
import io.openliberty.cdi30.internal.fat.apps.beansxml.SimpleBean;

@RunWith(FATRunner.class)
public class CDI30BeansXMLTest extends FATServletClient {

    public static final String SERVER_NAME = "CDI30Server";

    private static final String BEANS_XML_APP_NAME = "CDI30BeansXMLApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = CDI30BeansXMLTestServlet.class, contextRoot = BEANS_XML_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive cdiBeansXMLWar = ShrinkWrap.create(WebArchive.class, BEANS_XML_APP_NAME + ".war")
                                              .addClass(CDI30BeansXMLTestServlet.class.getName())
                                              .addClass(SimpleBean.class.getName())
                                              .addClass(AnnotatedBean.class.getName());
        CDIArchiveHelper.addBeansXML(cdiBeansXMLWar, DiscoveryMode.ALL, CDIVersion.CDI30);

        ShrinkHelper.exportDropinAppToServer(server, cdiBeansXMLWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
