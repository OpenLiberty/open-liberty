/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.passivation.tests;

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
        String test = testName.getMethodName();
        if (test.endsWith(RepeatTestFilter.getRepeatActionsAsString())) {
            test = test.replace(RepeatTestFilter.getRepeatActionsAsString(), "");
        }
        FATServletClient.runTest(getServer(), servlet, test);
    }

    protected void runTest(String servlet, String testMethod) throws Exception {
        FATServletClient.runTest(getServer(), servlet, testMethod);
    }

}
