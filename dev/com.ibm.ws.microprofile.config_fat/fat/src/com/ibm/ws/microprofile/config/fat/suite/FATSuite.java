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
import com.ibm.ws.microprofile.config.fat.tests.CDIBrokenInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIBuiltInConverterTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIConfigPropertyTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIFieldInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIScopeTest;
import com.ibm.ws.microprofile.config.fat.tests.CDIXtorInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoaderCacheTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoadersTest;
import com.ibm.ws.microprofile.config.fat.tests.ConverterPriorityTest;
import com.ibm.ws.microprofile.config.fat.tests.ConvertersTest;
import com.ibm.ws.microprofile.config.fat.tests.CustomSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.DefaultSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.DynamicSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.OrdinalsForDefaultsTest;
import com.ibm.ws.microprofile.config.fat.tests.SharedLibTest;
import com.ibm.ws.microprofile.config.fat.tests.SimultaneousRequestsTest;
import com.ibm.ws.microprofile.config.fat.tests.StressTest;
import com.ibm.ws.microprofile.config.fat.tests.TypesTest;

/**
 * Tests specific to appConfig
 */
@RunWith(Suite.class)
@SuiteClasses({

                CDIBrokenInjectionTest.class,
                CDIBuiltInConverterTest.class,
                CDIConfigPropertyTest.class,
                CDIFieldInjectionTest.class,
                CDIScopeTest.class,
                CDIXtorInjectionTest.class,
                ClassLoaderCacheTest.class,
                ClassLoadersTest.class,
                ConverterPriorityTest.class,
                ConvertersTest.class,
                CustomSourcesTest.class,
                DefaultSourcesTest.class,
                DynamicSourcesTest.class,
                OrdinalsForDefaultsTest.class,
                SimultaneousRequestsTest.class,
                SharedLibTest.class,
                StressTest.class,
                TypesTest.class,

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
