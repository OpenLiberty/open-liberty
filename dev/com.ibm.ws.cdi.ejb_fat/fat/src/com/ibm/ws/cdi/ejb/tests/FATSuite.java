/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AroundConstructEjbTest.class,
                CDIManagedBeanInterceptorTest.class,
                RemoteEJBTest.class,
                EjbConstructorInjectionTest.class,
                EjbDiscoveryTest.class,
                EjbScopeTest.class,
                EjbTimerTest.class,
                InjectParameterTest.class,
                InterceptorsTest.class,
                MdbTest.class,
                MultipleNamedEJBTest.class,
                StatefulSessionBeanInjectionTest.class,
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    public static RepeatTests defaultRepeat(String serverName) {
        //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
        return EERepeatActions.repeat(serverName, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE7);
    }

    public static RepeatTests defaultRepeatUpToEE10(String serverName) {
        //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
        return EERepeatActions.repeat(serverName, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);
    }

}
