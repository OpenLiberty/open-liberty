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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for globally enabling (across multiple bean archives) decorators, interceptors and
 * alternatives using {@code @Priority}.
 * 
 * The tests use two bean archives: a war and a library jar. Globally enabled decorators, interceptors and
 * alternatives should work across both bean archives.
 * <p>
 * The test servlet is {@code GlobalPriorityTestServlet}. Note that these tests use {@link FATServletClient}.
 * 
 * @see <a href="http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#_major_changes">CDI spec - Major changes</a>
 * @see <a href="http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#enabled_decorators_priority">Decorator enablement</a>
 */

@Mode(TestMode.FULL)
public class GloballyEnableUsingPriorityTest extends LoggingTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12GlobalPriorityServer");

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive globalPriorityLib = ShrinkWrap.create(JavaArchive.class,"globalPriorityLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.RelativePriority")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.Bean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.LocalJarInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.LocalJarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.FromJar")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarInterceptor")
                        .add(new FileAsset(new File("test-applications/globalPriorityLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive globalPriorityWebApp = ShrinkWrap.create(WebArchive.class, "globalPriorityWebApp.war")
                        .addClass("com.ibm.ws.cdi12.test.priority.NoPriorityBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.FromWar")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.GlobalPriorityTestServlet")
                        .add(new FileAsset(new File("test-applications/globalPriorityWebApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        EnterpriseArchive globalPriorityApp = ShrinkWrap.create(EnterpriseArchive.class,"globalPriorityApp.ear")
                        .add(new FileAsset(new File("test-applications/globalPriorityApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(globalPriorityLib)
                        .addAsLibrary(utilLib)
                        .addAsModule(globalPriorityWebApp);

        ShrinkHelper.exportDropinAppToServer(server, globalPriorityApp);
        server.waitForStringInLogUsingMark("CWWKZ0001I");

    }

    @ClassRule
    public static TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public TestRule runAll = TestRules.runAllUsingTestNames(server).onPath("globalPriorityWebApp/testServlet");

    /**
     * Test that a bean in a library jar is decorated by a globally enabled {@code @Decorator} in a war.
     */
    @Test
    public void testDecoratedJarBean() {}

    /**
     * Test that a bean in a war is decorated by a globally enabled {@code @Decorator} in a library jar.
     */
    @Test
    public void testDecoratedWarBean() {}

    /**
     * Test that two decorators with different priorities are called in the correct order.
     */
    @Test
    public void testPrioritizedDecoratorOrder() {}

    /**
     * Test that a high-priority {@code @Alternative} in a library jar takes precedence
     * over both a low-priority {@code @Alternative} and a non-alternative bean in the local war.
     */
    @Test
    public void testAlternativePriority() {}

    /**
     * Test that a bean in a war is intercepted by a globally enabled {@code @Interceptor} in a library jar.
     */
    @Test
    public void testInterceptedFromLibJar() {}

    /**
     * Test that two interceptors with different priorities are called in the correct order.
     */
    @Test
    public void testPrioritizedInterceptorOrder() {}

    /*************************************************************************
     * The tests below this line used to live in 'EnableUsingBeansXmlTest.java'
     *************************************************************************/

    /**
     * Test that a decorator enabled in beans.xml is enabled in the same archive.
     */
    @Test
    public void testLocalDecoratorEnabledForArchive() {}

    @Test
    public void testGlobalDecoratorsAreBeforeLocalDecorator() {}

    /**
     * Test that a decorator enabled in beans.xml is not enabled in a different archive.
     */
    @Test
    public void testLocalDecoratorsAreDisabledInOtherArchives() {}

    /**
     * Test that an interceptor enabled in beans.xml is enabled in the same archive.
     */
    @Test
    public void testLocalInterceptorEnabledForArchive() {}

    @Test
    public void testGlobalInterceptorsAreBeforeLocalInterceptor() {}

    /**
     * Test that a interceptor enabled in beans.xml is not enabled in a different archive.
     */
    @Test
    public void testLocalInterceptorsAreDisabledInOtherArchives() {}
}
