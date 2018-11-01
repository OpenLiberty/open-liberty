/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class CDI12WebServicesTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12WebServicesServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        WebArchive resourceWebServicesProvider = ShrinkWrap.create(WebArchive.class,
                                                                   "resourceWebServicesProvider.war").addClass("com.ibm.ws.cdi.services.impl.MyPojoUser").addClass("com.ibm.ws.cdi.services.impl.SayHelloPojoService").addClass("com.ibm.ws.cdi.services.SayHelloService").add(new FileAsset(new File("test-applications/resourceWebServicesProvider.war/resources/WEB-INF/web.xml")),
                                                                                                                                                                                                                                                                               "/WEB-INF/web.xml");

        WebArchive resourceWebServicesClient = ShrinkWrap.create(WebArchive.class,
                                                                 "resourceWebServicesClient.war").addClass("servlets.TestWebServicesServlet").addClass("client.services.SayHello_Type").addClass("client.services.SayHelloResponse").addClass("client.services.package-info").addClass("client.services.SayHello").addClass("client.services.ObjectFactory").addClass("client.services.SayHelloPojoService").add(new FileAsset(new File("test-applications/resourceWebServicesClient.war/resources/META-INF/resources/wsdl/EmployPojoService.wsdl")),
                                                                                                                                                                                                                                                                                                                                                                                                                 "/META-INF/resources/wsdl/EmployPojoService.wsdl");

        return ShrinkWrap.create(EnterpriseArchive.class,
                                 "resourceWebServices.ear").add(new FileAsset(new File("test-applications/resourceWebServices.ear/resources/META-INF/application.xml")),
                                                                "/META-INF/application.xml").addAsModule(resourceWebServicesClient).addAsModule(resourceWebServicesProvider);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Verifies that a @Resource can be injected and used in a WebService that resides in a CDI implicit bean archive
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testResourceInjectionForWSinImplicitBDA() throws Exception {

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {

                try {
                    verifyResponse("/resourceWebServicesClient/TestWebServicesServlet", "Hello, Bobby from mySecondName in SayHelloPojoService");
                } catch (Exception E) {

                }
                return null;

            }
        });
    }

}
