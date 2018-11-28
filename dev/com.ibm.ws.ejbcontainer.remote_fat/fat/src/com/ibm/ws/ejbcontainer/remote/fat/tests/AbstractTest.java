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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.junit.Rule;
import org.junit.rules.TestName;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public abstract class AbstractTest {

    public abstract LibertyServer getServer();

    @Rule
    public TestName testName = new TestName();

    protected void runTest(String servlet) throws Exception {
        String test = testName.getMethodName().endsWith(RepeatTestFilter.CURRENT_REPEAT_ACTION) ? testName.getMethodName().substring(0,
                                                                                                                                     testName.getMethodName().length()
                                                                                                                                        - (RepeatTestFilter.CURRENT_REPEAT_ACTION.length()
                                                                                                                                           + 1)) : testName.getMethodName();
        FATServletClient.runTest(getServer(), servlet, test);
    }

    protected void runTest(String servlet, String testMethod) throws Exception {
        FATServletClient.runTest(getServer(), servlet, testMethod);
    }

}
