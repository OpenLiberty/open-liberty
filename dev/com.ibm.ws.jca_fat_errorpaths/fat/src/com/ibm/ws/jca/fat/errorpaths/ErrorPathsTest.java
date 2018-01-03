/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.errorpaths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
@RunWith(FATRunner.class)
public class ErrorPathsTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat.errorpaths")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultRar(server, "ErrorPathRA", "com.ibm.test.errorpathadapter");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("J2CA9919W", // EXPECTED: The class loader for resource adapter ErrorPathRA is unable to load com.ibm.test.errorpathadapter.ImplDoesNotExist. Check if the resource adapter requires a feature that is not enabled or a library that is not accessible to it.
                              "CWWKG0032W", // EXPECTED: Unexpected value specified for property [month], value = [14]. Expected value(s) are: [0][1][2][3][4][5][6][7][8][9][10][11].
                              "CWWKG0058E", // EXPECTED: properties.ErrorPathRA with the unique identifier default-0 is missing required attribute requiredProp1
                              "CWWKG0075E", // EXPECTED: The value 1.01e-5 is not valid for attribute floatProp1 of configuration element adminObject. The validation message was: Value "1.01e-5" is out of range..
                              "CWWKG0076W");// EXPECTED: The previous configuration for adminObject with id default-6 is still in use. (This is because the new config wasn't valid)
        }
    }

    @Test
    public void testActivationSpec_MissingRequiredProp1() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0058E.* properties.ErrorPathRA .* requiredProp1.*")) {
            throw new Exception("Did not find warning (CWWKG0058E) for missing required attribute.");
        }
    }

    @Test
    public void testAdminObject_Date_Invalid() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0032W.*month.*14.*0.*1.*2.*3.*4.*5.*6.*7.*8.*9.*10.*11.*"))
            throw new Exception("Did not find warning for invalid option.");
    }

    @Test
    public void testMaxConstraintForByteAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 126 .*"))
            throw new Exception("Did not find warning for Byte value that exceeds max");
    }

    @Test
    public void testMinConstraintForByteAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 0 .*"))
            throw new Exception("Did not find warning for Byte value that is less than min");
    }

    @Test
    @Ignore
    public void testMaxConstraintForCharAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* \\{ .*"))
            throw new Exception("Did not find warning for Character value that exceeds max");
    }

    @Test
    @Ignore
    public void testMinConstraintForCharAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* @ .*"))
            throw new Exception("Did not find warning for Character value that is less than min");
    }

    @Test
    public void testMaxConstraintForDoubleAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 100\\.1 .*"))
            throw new Exception("Did not find warning for Double value that exceeds max");
    }

    @Test
    public void testMinConstraintForDoubleAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 9\\.9 .*"))
            throw new Exception("Did not find warning for Double value that is less than min");
    }

    @Test
    public void testMaxConstraintForFloatAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 1\\.01e\\-5 .*"))
            throw new Exception("Did not find warning for Float value that exceeds max");
    }

    @Test
    public void testMinConstraintForFloatAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 9\\.0e\\-8 .*"))
            throw new Exception("Did not find warning for Float value that is less than min");
    }

    @Test
    public void testMaxConstraintForIntAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 0 .*"))
            throw new Exception("Did not find warning for Integer value that exceeds max");
    }

    @Test
    public void testMinConstraintForIntAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* \\-2147433333 .*"))
            throw new Exception("Did not find warning for Integer value that is less than min");
    }

    @Test
    public void testMinConstraintForLongAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* \\-1 .*"))
            throw new Exception("Did not find warning for Long value that is less than min");
    }

    @Test
    public void testMaxConstraintForShortAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* 9223372036854775807 .*"))
            throw new Exception("Did not find warning for Short value that exceeds max");
    }

    @Test
    public void testMinConstraintForShortAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* \\-999999999 .*"))
            throw new Exception("Did not find warning for Short value that is less than min");
    }

    @Test
    public void testMaxConstraintForStringAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* aaabbbccc .*"))
            throw new Exception("Did not find warning for String value that exceeds max");
    }

    @Test
    public void testMinConstraintForStringAttribute() throws Exception {
        if (null == server.waitForStringInLog(".*CWWKG0075E.* ABCDE .*"))
            throw new Exception("Did not find warning for String value that is less than min");
    }

    @Test
    public void testUnavailableAdminObjectInterface() throws Exception {
        if (null == server.waitForStringInLog(".*J2CA9919W.*com.ibm.test.errorpathadapter.InterfaceDoesNotExist.*"))
            throw new Exception("Did not find warning for unavailable interface");
    }
}
