/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class AroundConstructEjbTest extends FATServletClient {

    public static final String AROUND_CONSTRUCT_APP_NAME = "aroundConstructApp";
    public static final String POST_CONSTRUCT_ERROR_APP_NAME = "postConstructErrorMessageApp";
    public static final String SERVER_NAME = "cdi12EJB32Server";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.aroundconstruct.EjbServlet.class, contextRoot = AROUND_CONSTRUCT_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.aroundconstruct.BeanServlet.class, contextRoot = AROUND_CONSTRUCT_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,
                                                "utilLib.jar")
                                        .addClass(com.ibm.ws.cdi.ejb.utils.Intercepted.class)
                                        .addClass(com.ibm.ws.cdi.ejb.utils.Utils.class)
                                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")),
                                             "/META-INF/beans.xml");

        WebArchive aroundConstructApp = ShrinkWrap.create(WebArchive.class,
                                                          AROUND_CONSTRUCT_APP_NAME + ".war")
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.StatelessAroundConstructLogger.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.Ejb.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.Bean.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.SuperConstructInterceptor.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorTwoBinding.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.DirectlyIntercepted.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorOne.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.SubConstructInterceptor.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.DirectBindingConstructInterceptor.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.NonCdiInterceptor.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.ConstructInterceptor.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorOneBinding.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorTwo.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.EjbServlet.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.BeanServlet.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructTestServlet.class)
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.aroundconstruct.StatelessEjb.class)
                                                  .add(new FileAsset(new File("test-applications/" + AROUND_CONSTRUCT_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                       "/WEB-INF/beans.xml")
                                                  .addAsLibrary(utilLib);

        WebArchive postConstructErrorMessageApp = ShrinkWrap.create(WebArchive.class,
                                                                    POST_CONSTRUCT_ERROR_APP_NAME + ".war")
                                                            .addClass(com.ibm.ws.cdi.ejb.apps.postConstructError.ErrorMessageServlet.class)
                                                            .addClass(com.ibm.ws.cdi.ejb.apps.postConstructError.interceptors.ErrorMessageInterceptor.class)
                                                            .addClass(com.ibm.ws.cdi.ejb.apps.postConstructError.interceptors.ErrorMessageInterceptorBinding.class)
                                                            .addClass(com.ibm.ws.cdi.ejb.apps.postConstructError.ErrorMessageTestEjb.class)
                                                            .add(new FileAsset(new File("test-applications/" + POST_CONSTRUCT_ERROR_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                                 "/WEB-INF/beans.xml")
                                                            .addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, aroundConstructApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, postConstructErrorMessageApp, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @Test
    @AllowedFFDC({ "javax.ejb.EJBException", "java.lang.reflect.UndeclaredThrowableException", "java.lang.IllegalStateException" })
    public void testPostConstructErrorMessage() {
        int errMsgCount = 0;

        try {
            HttpUtils.findStringInUrl(server, "/postConstructErrorMessageApp/errorMessageTestServlet", " "); //Just to poke the url
        } catch (Throwable e1) {
            //The request fails with HTTP status 500, that triggers an AssertionFailedError in HttpUtils. Since we're looking for an error in the logs I believe a status 500 is expected behaviour.
        }
        try {
            errMsgCount = server.findStringsInLogs("CWOWB2001E(?=.*POST_CONSTRUCT)(?=.*java.lang.IllegalStateException)").size();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }

        assertTrue("The expected error message stating that an interceptor lifecycle callback threw an exception did not appear", errMsgCount > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWOWB2001E", "CNTR0019E", "SRVE0777E", "SRVE0315E");
        }
    }

}
