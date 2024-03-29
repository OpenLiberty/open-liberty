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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public abstract class AbstractTest {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                System.runFinalization();
                System.gc();
                getServer().serverDump("heap");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

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
