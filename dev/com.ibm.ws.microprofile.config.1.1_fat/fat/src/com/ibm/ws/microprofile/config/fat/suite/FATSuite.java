/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.microprofile.config.fat.tests.BasicConfigTests;
import com.ibm.ws.microprofile.config.fat.tests.CDIBrokenInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoaderCacheTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoadersTest;
import com.ibm.ws.microprofile.config.fat.tests.DefaultSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.DynamicSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.LibertySpecificConfigTests;
import com.ibm.ws.microprofile.config.fat.tests.OrdinalsForDefaultsTest;
import com.ibm.ws.microprofile.config.fat.tests.SharedLibTest;
import com.ibm.ws.microprofile.config.fat.tests.SimultaneousRequestsTest;
import com.ibm.ws.microprofile.config.fat.tests.StressTest;

/**
 * Tests specific to appConfig
 *
 * BasicConfigTests repeats across all MP Config versions (EE8)
 * the rest repeat against the lastest version of MP Config (where appropriate) and then one other combination of MP Config and EE version
 * the aim is that each combination is used to test at least once, across all of the MP Config FAT buckets
 * some classes do not repeat against the latest due to functional changes between MP Config 1.4 -> 2.0
 */
@RunWith(Suite.class)
@SuiteClasses({
                BasicConfigTests.class, //LITE
                ClassLoadersTest.class, //FULL
                DefaultSourcesTest.class, //FULL
                OrdinalsForDefaultsTest.class, //FULL
                SimultaneousRequestsTest.class, //FULL
                SharedLibTest.class, //FULL
                StressTest.class, //FULL

                // The following don't repeat against mpConfig > 1.4. See classes for why.
                LibertySpecificConfigTests.class, //FULL
                CDIBrokenInjectionTest.class, //FULL
                ClassLoaderCacheTest.class, //FULL
                DynamicSourcesTest.class //FULL

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
