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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi40.internal.fat.config.beansxml.AllBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.AnnotatedBeansServlet;
import io.openliberty.cdi40.internal.fat.config.beansxml.RequestScopedBean;
import io.openliberty.cdi40.internal.fat.config.beansxml.UnannotatedBean;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class BeansXMLTest extends FATServletClient {
    public static final String SERVER_NAME = "CDI40BeansXMLServer";

    public static final String EMPTY_BEANS_APP_NAME = "EmptyBeans";
    public static final String BEANS10_APP_NAME = "Beans10";
    public static final String ANNOTATED_BEANS_APP_NAME = "AnnotatedBeans";
    public static final String ALL_BEANS_APP_NAME = "AllBeans";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AllBeansServlet.class, contextRoot = ALL_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = EMPTY_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = ANNOTATED_BEANS_APP_NAME),
                    @TestServlet(servlet = AnnotatedBeansServlet.class, contextRoot = BEANS10_APP_NAME)
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
        //When using CDI 4.0, a v1.0 beans.xml means that UnannotatedBean should not be found
        WebArchive beans10XMLWar = ShrinkWrap.create(WebArchive.class, BEANS10_APP_NAME + ".war")
                                             .addClass(AnnotatedBeansServlet.class)
                                             .addClass(UnannotatedBean.class)
                                             .addClass(RequestScopedBean.class);
        //A CDI 1.0 beans.xml does not really have a discovery mode.
        //Before CDI 4.0, we treated this as meaning ALL
        //In CDI 4.0 this would now default to a discovery mode of ANNOTATED
        CDIArchiveHelper.addBeansXML(beans10XMLWar, CDIVersion.CDI10);
        ShrinkHelper.exportDropinAppToServer(server, beans10XMLWar, DeployOptions.SERVER_ONLY);

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
