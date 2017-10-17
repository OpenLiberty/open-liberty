package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.topology.utils.FATServletClient;

/**
 * Tests for having one EJB implementation class with two different {@code ejb-name}s declared in {@code ejb-jar.xml}.
 */
public class MultipleNamedEJBTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EJB32Server");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public final TestName testName = new TestName();

    private final void runTest() throws Exception {
        FATServletClient.runTest(SHARED_SERVER.getLibertyServer(), "multipleEJBsSingleClass", testName);
    }

    /**
     * Test that the two injected EJBs with different names are actually different instances.
     */
    @Test
    public void testEjbsAreDifferentInstances() throws Exception {
        this.runTest();
    }

    /**
     * Test that EJB wrapper class names include the correct EJB bean names.
     */
    @Test
    public void testWrapperClassNamesIncludeBeanName() throws Exception {
        this.runTest();
    }

    /**
     * Test that the 'enterprise bean name' used internally matches the name declared at the injection point.
     */
    @Test
    public void testInternalEnterpriseBeanNames() throws Exception {
        this.runTest();
    }

    /**
     * Test that the two EJBs are actually using different instances of the implementation by storing different state in each of them.
     */
    @Test
    public void testStateIsStoredSeparately() throws Exception {
        this.runTest();
    }

}
