/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.fat.suite;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.microprofile.config.fat.tests.BasicConfigTests;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoadersTest;
import com.ibm.ws.microprofile.config.fat.tests.DefaultSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.SharedLibTest;

/**
 * Tests specific to appConfig
 *
 * BasicConfigTests repeats across all MP Config versions
 * the rest repeat against the lastest version of MP Config (where appropriate) and then one other combination of MP Config and EE version
 * the aim is that each combination is used to test at least once, across all of the MP Config FAT buckets
 * some classes do not repeat against the latest due to functional changes between MP Config 1.4 -> 2.0
 *
 * Tests moved to com.ibm.ws.microprofile.config.1.1_fat_two:
 *   OrdinalsForDefaultsTest, SimultaneousRequestsTest, VisibilityTest,
 *   LibertySpecificConfigTests, CDIBrokenInjectionTest, ClassLoaderCacheTest, DynamicSourcesTest
 *
 * Tests moved to com.ibm.ws.microprofile.config.1.1_fat_stress:
 *   StressTest
 */
@RunWith(Suite.class)
@SuiteClasses({
                BasicConfigTests.class, //LITE
                ClassLoadersTest.class, //FULL
                DefaultSourcesTest.class, //FULL
                SharedLibTest.class //FULL
})

public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
