/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                NonContextualTests.class,
                VetoAndExtendTests.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null,
                                                         EERepeatActions.EE7,
                                                         EERepeatActions.EE10,
                                                         EERepeatActions.EE9,
                                                         EERepeatActions.EE8);

}
