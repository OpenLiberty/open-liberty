/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.junit.ClassRule;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        DummyTest.class,

        DelayLiteSecOffTest.class,
        DelayLiteSecOnTest.class,

        DelayFullSecOffTest.class,
        DelayFullSecOnTest.class,

        DelayFullTest.class
})
public class FATSuite {
     @ClassRule
     public static RepeatTests repeater = RepeatTests.withoutModification()
                                                     .andWith(FeatureReplacementAction.EE9_FEATURES())
                                                     .andWith(FeatureReplacementAction.EE10_FEATURES())
                                                     .andWith(FeatureReplacementAction.EE11_FEATURES());
}
