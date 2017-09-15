/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.fat;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class ProductInfoValidateTest {

    public static Class<?> c = ProductInfoValidateTest.class;
    //don't really need a server for this test, but need to get the install paths
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.product.utility.test.validate.server");

    private static String installRoot;

    @BeforeClass
    public static void setup() throws Exception {
        final String METHOD_NAME = "setup";
        installRoot = server.getInstallRoot();
        Log.entering(c, METHOD_NAME);
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);
    }

    /**
     * Tests that we can run the productInfo validate command without exception.
     * Does not verify the output of the command, only that there were no exceptions
     * during the run of the command.
     * 
     * @throws Exception
     */
    @Test
    public void testProductInfoValidate() throws Exception {
        final String METHOD_NAME = "testProductInfoValidate";
        Log.entering(c, METHOD_NAME);
        String cmd = installRoot + "/bin/productInfo";
        String[] parms = new String[] { "validate" };

        ProgramOutput po = server.getMachine().execute(cmd, parms, installRoot);
        Log.info(c, METHOD_NAME, "productInfo validate stdout: ");
        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, "productInfo validate stderr: ");
        Log.info(c, METHOD_NAME, po.getStderr());
        assertTrue("The productInfo validate command returned an error code, see autoFVT/results/output.txt log for detailed output", po.getReturnCode() == 0);

        Log.exiting(c, METHOD_NAME);
    }

}
