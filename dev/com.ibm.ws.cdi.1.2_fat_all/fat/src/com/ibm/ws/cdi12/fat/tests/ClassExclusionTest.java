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

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
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
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12ClassExclusionTestServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
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
