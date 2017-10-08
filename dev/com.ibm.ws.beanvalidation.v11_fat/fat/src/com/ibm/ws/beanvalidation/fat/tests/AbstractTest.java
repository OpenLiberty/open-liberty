/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.fat.tests;

import org.junit.Rule;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class AbstractTest {
    protected static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    /**
     * Use the junit @Rule TestName construct to get the test name on the test
     * runner side. To enable running the same test against multiple feature
     * versions the tests have a version qualifier at the end of each test method.
     * The test method in the application, however is the same as it is the same
     * application used, so here we remove the version qualifier before sending
     * the test request.
     *
     * <p>
     *
     * ex. testDefaultBuildDefaultValidatorFactory10 in the test runner ->
     * testDefaultBuildDefaultValidatorFactory in the web application
     */
    protected void run(String war, String servlet) throws Exception {
        String originalTestName = testName.getMethodName();
        String servletTest = originalTestName.substring(0, originalTestName.length() - 2);
        run(war, servlet, servletTest);
    }

    /**
     * Run a test by connecting to a url that is put together with the context-root
     * being the war, the servlet and test method in the web application.
     */
    protected void run(String war, String servlet, String testMethod) throws Exception {
        FATServletClient.runTest(server, war + "/" + servlet, testMethod);
    }
}
