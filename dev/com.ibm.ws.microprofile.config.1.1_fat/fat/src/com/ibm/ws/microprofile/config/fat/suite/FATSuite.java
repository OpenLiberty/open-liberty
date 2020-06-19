/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
import com.ibm.ws.microprofile.config.fat.tests.OrdinalsForDefaultsTest;
import com.ibm.ws.microprofile.config.fat.tests.SharedLibTest;
import com.ibm.ws.microprofile.config.fat.tests.SimultaneousRequestsTest;
import com.ibm.ws.microprofile.config.fat.tests.StressTest;

/**
 * Tests specific to appConfig
 */
@RunWith(Suite.class)
@SuiteClasses({
                BasicConfigTests.class, //LITE
                CDIBrokenInjectionTest.class, //FULL
                ClassLoaderCacheTest.class, //FULL
                ClassLoadersTest.class, //FULL
                DefaultSourcesTest.class, //FULL
                DynamicSourcesTest.class, //FULL
                OrdinalsForDefaultsTest.class, //FULL
                SimultaneousRequestsTest.class, //FULL
                SharedLibTest.class, //FULL
                StressTest.class //FULL

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
