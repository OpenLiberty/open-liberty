/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ProgrammaticLoginWithServerSecurityTest extends ProgrammaticLoginTest {
	private static final Class<?> c = ProgrammaticLoginWithServerSecurityTest.class;

    @BeforeClass
    public static void theBeforeClass() throws Exception {
        String thisMethod = "before";
        Log.info(c, thisMethod, "Performing server setup (ProgrammaticLoginWithServerSecurityTest)");
        try {
            commonServerSetUp("SecureServerTest", true);
        } catch (Exception e) {
            Log.info(c, thisMethod, "Server setup failed, tests will not run: " + e.getMessage());
            throw (new Exception("Server setup failed, tests will not run: " + e.getMessage(), e));
        }

        Log.info(c, thisMethod, "Server setup is complete");
    };

    @AfterClass
    public static void theAfterClass() {
        try {
        	Log.info(c, "after", "Stopping server process (ProgrammaticLoginWithServerSecurityTest)");
        	testServer.stopServer("CWWKZ0124E: Application testmarker does not contain any modules.");
        } catch (Exception e) {
            Log.info(c, "after", "Exception thrown in after " + e.getMessage());
            e.printStackTrace();
        }
    };

}
