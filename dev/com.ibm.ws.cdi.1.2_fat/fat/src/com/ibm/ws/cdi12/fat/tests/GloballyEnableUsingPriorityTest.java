/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.fat.util.LoggingTest;


import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;

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
    protected ShutDownSharedServer getSharedServer() {
        return null;
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
