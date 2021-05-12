/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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

import com.ibm.ws.cdi12.fat.tests.AfterTypeDiscoveryTest;
import com.ibm.ws.cdi12.fat.tests.BeanDiscoveryModeNoneTest;
import com.ibm.ws.cdi12.fat.tests.ClassExclusionTest;
import com.ibm.ws.cdi12.fat.tests.CustomerProvidedXMLParserFactoryTest;
import com.ibm.ws.cdi12.fat.tests.DisablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.EmptyCDITest;
import com.ibm.ws.cdi12.fat.tests.EnablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.MultipleBeansXmlTest;
import com.ibm.ws.cdi12.fat.tests.WebBeansBeansXmlInWeldTest;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AfterTypeDiscoveryTest.class,
                BeanDiscoveryModeNoneTest.class,
                ClassExclusionTest.class,
                CustomerProvidedXMLParserFactoryTest.class,
                DisablingBeansXmlValidationTest.class,
                EmptyCDITest.class,
                EnablingBeansXmlValidationTest.class,
                MultipleBeansXmlTest.class,
                WebBeansBeansXmlInWeldTest.class,
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES()).andWith(FeatureReplacementAction.EE9_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
