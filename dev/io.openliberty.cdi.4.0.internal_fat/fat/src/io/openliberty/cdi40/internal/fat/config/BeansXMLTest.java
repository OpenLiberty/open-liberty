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
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi40.internal.fat.config.beansxml.AllBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.AnnotatedBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;

@RunWith(FATRunner.class)
public class BeansXMLTest {
    public static final String SERVER_NAME = "CDI40Server";

    public static final String EMPTY_BEANS_APP_NAME = "EmptyBeans";
    public static final String ANNOTATED_BEANS_APP_NAME = "AnnotatedBeans";
    public static final String ALL_BEANS_APP_NAME = "AllBeans";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AllBeansServlet.class, contextRoot = ALL_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = EMPTY_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = ANNOTATED_BEANS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //AnnotatedBeansServlet expects not to find a UnannotatedBean bean
        //an empty beans.xml means that UnannotatedBean should not be found
        WebArchive emptyBeansXMLWar = ShrinkWrap.create(WebArchive.class, EMPTY_BEANS_APP_NAME + ".war")
                                                .addClass(AnnotatedBeansServlet.class)
                                                .addClass(UnannotatedBean.class)
                                                .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addEmptyBeansXML(emptyBeansXMLWar);
        ShrinkHelper.exportDropinAppToServer(server, emptyBeansXMLWar, DeployOptions.SERVER_ONLY);

        //AnnotatedBeansServlet expects not to find a UnannotatedBean bean
        WebArchive annotatedBeansXMLWar = ShrinkWrap.create(WebArchive.class, ANNOTATED_BEANS_APP_NAME + ".war")
                                                    .addClass(AnnotatedBeansServlet.class)
                                                    .addClass(UnannotatedBean.class)
                                                    .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addBeansXML(annotatedBeansXMLWar, DiscoveryMode.ANNOTATED, CDIVersion.CDI40);
        ShrinkHelper.exportDropinAppToServer(server, annotatedBeansXMLWar, DeployOptions.SERVER_ONLY);

        //AllBeansServlet expects to find a UnannotatedBean bean
        WebArchive allBeansXMLWar = ShrinkWrap.create(WebArchive.class, ALL_BEANS_APP_NAME + ".war")
                                              .addClass(AllBeansServlet.class)
                                              .addClass(UnannotatedBean.class)
                                              .addClass(RequestScopedBean.class);
        CDIArchiveHelper.addBeansXML(allBeansXMLWar, DiscoveryMode.ALL, CDIVersion.CDI40);
        ShrinkHelper.exportDropinAppToServer(server, allBeansXMLWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
