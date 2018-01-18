/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for the <code>@AroundConstruct</code> lifecycle callback, defined in Interceptors 1.2.
 */
public abstract class AroundConstructTestBase extends LoggingTest {

    protected abstract String getServletName();

    private static boolean hasSetup = false;
    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12EJB32Server");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return null;
    }

    public static void setUp() throws Exception {

        if (hasSetup){ 
            return;
        }
        hasSetup = true;

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive aroundConstructApp = ShrinkWrap.create(WebArchive.class, "aroundConstructApp.war")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.StatelessAroundConstructLogger")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.Ejb")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.Bean")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.SuperConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwoBinding")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectlyIntercepted")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOne")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.SubConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectBindingConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.NonCdiInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.ConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOneBinding")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwo")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.EjbServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.BeanServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructTestServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.StatelessEjb")
                        .add(new FileAsset(new File("test-applications/aroundConstructApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(utilLib);

        WebArchive postConstructErrorMessageApp = ShrinkWrap.create(WebArchive.class, "postConstructErrorMessageApp.war")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.ErrorMessageServlet")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.interceptors.ErrorMessageInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.interceptors.ErrorMessageInterceptorBinding")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.ErrorMessageTestEjb")
                        .add(new FileAsset(new File("test-applications/postConstructErrorMessageApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, aroundConstructApp);
        ShrinkHelper.exportDropinAppToServer(server, postConstructErrorMessageApp);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application aroundConstructApp started");
    }

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("aroundConstructApp").andServlet(getServletName());

    @Test
    public void testBasicAroundConstruct() {}

    /**
     * Test that AroundConstruct works for Non CDI interceptors
     */
    @Test
    public void testNonCdiAroundConstruct() {}

    /**
     * Interceptors 1.2 - "AroundConstruct lifecycle callback interceptor methods may be defined on superclasses of interceptor
     * classes."
     */
    @Test
    public void testAroundConstructInSuperClass() {}

    /**
     * Test that intercepting a constructor annotated with <code>@Inject</code> works.
     */
    @Test
    public void testInjectionConstructorIsCalled() {}

    /**
     * Interceptors 1.2 - "The getConstructor method returns the constructor of the target class for which the AroundConstruct
     * interceptor was invoked."
     */
    @Test
    public void testGetConstructor() {}

    /**
     * Interceptors 1.2 - "The getTarget method returns the associated target instance. For the AroundConstruct lifecycle
     * callback interceptor method, getTarget returns null if called before the proceed method returns."
     */
    @Test
    public void testGetTarget() {}

    /**
     * Test that we can apply an interceptor binding annotation directly to a constructor rather than the class.
     */
    @Test
    public void testBindingInterceptorToConstructor() {}

    /**
     * Interceptors should be called in the correct order as determined by the @Priority annotation and the order declared in the beans.xml
     */
    @Test
    public void testInterceptorOrder() {}

    /**
     * Interceptors should only be called once for each constructor
     */
    @Test
    public void testInterceptorNotCalledTwice() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
