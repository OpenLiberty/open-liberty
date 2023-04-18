/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ear.ImplicitBeanArchiveDisabledApp;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ear.explicitBeanArchive.MyBike;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ear.war.MyCar;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ear.war.MyCarServlet;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchivesDisabled.ear.war.jar.MyPlane;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ImplicitBeanArchivesDisabledTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12DisableImplicitBeanArchiveServer";

    public static final String IMPLICIT_BEAN_ARCHIVE_DISABLED_APP_NAME = "implicitBeanArchiveDisabled";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = MyCarServlet.class, contextRoot = IMPLICIT_BEAN_ARCHIVE_DISABLED_APP_NAME) }) //LITE
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        //implicit bean archives are disabled in the server.xml
        //this jar does not have a beans.xml so MyPlane should not be found as a Bean
        JavaArchive implicitBeanArchiveDisabledJar = ShrinkWrap.create(JavaArchive.class, IMPLICIT_BEAN_ARCHIVE_DISABLED_APP_NAME + ".jar");
        implicitBeanArchiveDisabledJar.addClass(MyPlane.class);

        //this war does have a beans.xml with mode=annotated so MyCar will be found as a Bean
        WebArchive implicitBeanArchiveDisabled = ShrinkWrap.create(WebArchive.class, IMPLICIT_BEAN_ARCHIVE_DISABLED_APP_NAME + ".war");
        implicitBeanArchiveDisabled.addClass(MyCar.class);
        implicitBeanArchiveDisabled.addClass(MyCarServlet.class);
        CDIArchiveHelper.addBeansXML(implicitBeanArchiveDisabled, MyCarServlet.class.getPackage(), "annotated-beans.xml");
        implicitBeanArchiveDisabled.addAsLibrary(implicitBeanArchiveDisabledJar);

        //this jar does have a empty beans.xml so MyBike will be found as a Bean
        JavaArchive explicitBeanArchive = ShrinkWrap.create(JavaArchive.class, "explicitBeanArchive.jar");
        explicitBeanArchive.addClass(MyBike.class);
        CDIArchiveHelper.addBeansXML(explicitBeanArchive, MyBike.class.getPackage(), "empty-beans.xml");

        EnterpriseArchive implicitBeanArchiveDisabledEar = ShrinkWrap.create(EnterpriseArchive.class, IMPLICIT_BEAN_ARCHIVE_DISABLED_APP_NAME + ".ear");
        implicitBeanArchiveDisabledEar.setApplicationXML(ImplicitBeanArchiveDisabledApp.class.getPackage(), "application.xml");
        implicitBeanArchiveDisabledEar.addAsLibrary(explicitBeanArchive);
        implicitBeanArchiveDisabledEar.addAsModule(implicitBeanArchiveDisabled);

        ShrinkHelper.exportDropinAppToServer(server, implicitBeanArchiveDisabledEar, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testWarningMessage() throws Exception {
        List<String> msgs = server.findStringsInLogs("CWOWB1009W: Implicit bean archives are disabled.");
        assertTrue("Message not found in logs: CWOWB1009W: Implicit bean archives are disabled.", msgs.size() > 0);
        assertEquals("Message CWOWB1009W was found more than once", 1, msgs.size());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        /*
         * Ignore following warnings as they are expected:
         * CWOWB1009W: Implicit bean archives are disabled.
         */
        server.stopServer("CWOWB1009W");
    }

}
