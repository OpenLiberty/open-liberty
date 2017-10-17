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
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests verify that you can exclude classes from Bean discovery through beans.xml and the @Vetoed annotaiton as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#bean_discovery
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#what_classes_are_beans
 */

@Mode(TestMode.FULL)
public class ClassExclusionTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12ClassExclusionTestServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testIncludedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "IncludedBean was correctly injected");
    }

    @Test
    public void testExcludedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedBean was correctly rejected");
    }

    @Test
    public void testExcludedPackageBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedPackageBean was correctly rejected");
    }

    @Test
    public void testExcludedPackageTreeBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedPackageTreeBean was correctly rejected");
    }

    @Test
    public void testProtectedByClassBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ProtectedByClassBean was correctly injected");
    }

    @Test
    public void testExcludedByPropertyBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedByPropertyBean was correctly rejected");
    }

    @Test
    public void testExcludedByComboBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedByComboBean was correctly rejected");
    }

    @Test
    public void testProtectedByHalfComboBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ProtectedByHalfComboBean was correctly injected");
    }

    @Test
    public void testVetoedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "VetoedBean was correctly rejected");
    }

    @Test
    public void testVetoedAlternativeDoesntThrowException() throws Exception {
        this.verifyResponse("/TestVetoedAlternative/testservlet", "Hello");
    }

}
