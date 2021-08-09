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
package com.ibm.ws.cdi12.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi12.fat.tests.BeanLifecycleTest;
import com.ibm.ws.cdi12.fat.tests.EventMetaDataTest;
import com.ibm.ws.cdi12.fat.tests.ObservesInitializedTest;
import com.ibm.ws.cdi12.fat.tests.PassivationBeanTests;
import com.ibm.ws.cdi12.fat.tests.SessionDestroyTests;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                BeanLifecycleTest.class,
                EventMetaDataTest.class,
                ObservesInitializedTest.class,
                PassivationBeanTests.class,
                SessionDestroyTests.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES()) // run all tests as-is (e.g. EE8 features)
                    .andWith(new JakartaEE9Action());

    /**
     * @throws Exception
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() throws Exception {
        FatLogHandler.generateHelpFile();
    }

}
