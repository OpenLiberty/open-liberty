/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.microprofile.config.fat.tests.CDIBrokenMethodInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIBrokenXtorInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIBuiltInConverterTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIConfigPropertyTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIFieldInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIMethodInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIMissingConvertersTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIScopeTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIXtorInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoadersTest;
import com.ibm.ws.microprofile.config.fat.tests.ConverterPriorityTest;
import com.ibm.ws.microprofile.config.fat.tests.ConvertersTest;
import com.ibm.ws.microprofile.config.fat.tests.CustomSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.DefaultSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.DynamicSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.OrdinalsForDefaultsTest;
import com.ibm.ws.microprofile.config.fat.tests.SharedLibTest;
import com.ibm.ws.microprofile.config.fat.tests.StressTest;
import com.ibm.ws.microprofile.config.fat.tests.TypesTest;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to appConfig
 */
@RunWith(Suite.class)
@SuiteClasses({

                CDIBrokenXtorInjectionTest.class,
                CDIBrokenMethodInjectionTest.class,
                CDIBuiltInConverterTest.class,
                CDIConfigPropertyTest.class,
                CDIFieldInjectionTest.class,
                CDIMethodInjectionTest.class,
                CDIMissingConvertersTest.class,
                CDIScopeTest.class,
                CDIXtorInjectionTest.class,
                ClassLoadersTest.class,
                ConverterPriorityTest.class,
                ConvertersTest.class,
                CustomSourcesTest.class,
                DefaultSourcesTest.class,
                DynamicSourcesTest.class,
                OrdinalsForDefaultsTest.class,
                SharedLibTest.class,
                StressTest.class,
                TypesTest.class,

})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("mpConfig-1.1", "mpConfig-1.2"));

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
