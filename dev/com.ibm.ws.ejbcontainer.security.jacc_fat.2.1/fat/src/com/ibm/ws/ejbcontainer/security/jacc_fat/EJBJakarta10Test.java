/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;

/**
 * Base class for EJBJakarta10. This class is extended for EJB in WAR testing.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class EJBJakarta10Test extends EJBJakarta10Base {

    protected static Class<?> logClass = EJBJakarta10Test.class;

    @Rule
    public TestName name = new TestName();

    @ClassRule
    public static RepeatTests r = FATSuite.defaultAndCheckpointRepeat(Constants.SERVER_EJBJAR);

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(logClass, Constants.SERVER_EJBJAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR, Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}
