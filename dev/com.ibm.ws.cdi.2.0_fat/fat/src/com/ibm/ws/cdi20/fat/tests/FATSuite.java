/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncEventsTest.class,
                BasicCdi20Tests.class,
                BuiltinAnnoLiteralsTest.class,
                CDIContainerConfigTest.class,
                SecureAsyncEventsTest.class,
                StartupEventsTest.class
})
public class FATSuite {

    public static RepeatTests defaultRepeat(String serverName) {
        return EERepeatActions.repeat(serverName, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE8);
    }

}
