/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi12.fat.tests.AlterableContextTest;
import com.ibm.ws.cdi12.fat.tests.CDICurrentTest;
import com.ibm.ws.cdi12.fat.tests.ConversationFilterTest;
import com.ibm.ws.cdi12.fat.tests.InjectInjectionPointAsParamTest;
import com.ibm.ws.cdi12.fat.tests.InjectInjectionPointBeansXMLTest;
import com.ibm.ws.cdi12.fat.tests.InjectInjectionPointTest;
import com.ibm.ws.cdi12.fat.tests.InjectInjectionPointXMLTest;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AlterableContextTest.class,
                CDICurrentTest.class,
                ConversationFilterTest.class,
                InjectInjectionPointTest.class,
                InjectInjectionPointBeansXMLTest.class,
                InjectInjectionPointXMLTest.class,
                InjectInjectionPointAsParamTest.class,
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES()).andWith(FeatureReplacementAction.EE9_FEATURES());

}
