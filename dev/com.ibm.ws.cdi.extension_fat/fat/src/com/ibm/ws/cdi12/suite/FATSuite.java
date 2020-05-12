/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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

import com.ibm.ws.cdi12.fat.tests.AppExtensionTest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionSPITest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionTest;
import com.ibm.ws.cdi12.fat.tests.DynamicBeanExtensionTest;
import com.ibm.ws.cdi12.fat.tests.ObserverTest;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AppExtensionTest.class,
                CDI12ExtensionTest.class,
                CDI12ExtensionSPITest.class,
                DynamicBeanExtensionTest.class,
                ObserverTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
