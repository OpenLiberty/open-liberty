/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.javamail.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({ IMAPTest.class,
                POP3Test.class,
                SMTPTest.class,
                MailSessionInjectionTest.class,
                MutuallyExclusive.class
})
public class FATSuite {

    private static LibertyServer mailSesionServer = LibertyServerFactory.getLibertyServer("mailSessionTestServer");
    private static LibertyServer javamailfat = LibertyServerFactory.getLibertyServer("com.ibm.ws.javamail.fat");

    @BeforeClass
    public static void setupApp() throws Exception {
        WebArchive testingApp = ShrinkWrap.create(WebArchive.class, "TestingApp.war")
                        .addPackages(true, "TestingApp/POP3")
                        .addPackages(true, "TestingApp/IMAP")
                        .addPackages(true, "TestingApp/SMTP")
			            .addPackages(true, "TestingApp/web")
                        .addAsManifestResource(new File("test-applications/TestingApp/resources/META-INF/MANIFEST.MF"))
                        .addAsManifestResource(new File("test-applications/TestingApp/resources/META-INF/permissions.xml"));
		ShrinkHelper.exportAppToServer(mailSesionServer, testingApp);


        WebArchive fvtweb = ShrinkWrap.create(WebArchive.class, "fvtweb.war")
                        .addPackages(true, "fvtweb/web", "fvtweb/ejb")
                        .addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ejb-jar.xml"))
                        .addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-ejb-jar-bnd.xml"))
                        .addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"))
                        .addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"))
                        .addAsManifestResource(new File("test-applications/fvtweb/resources/META-INF/permissions.xml"));

        WebArchive fvtear = ShrinkWrap.create(WebArchive.class, "fvtapp.ear").addAsLibrary(fvtweb);
        ShrinkHelper.exportAppToServer(javamailfat, fvtear);

    }
}
