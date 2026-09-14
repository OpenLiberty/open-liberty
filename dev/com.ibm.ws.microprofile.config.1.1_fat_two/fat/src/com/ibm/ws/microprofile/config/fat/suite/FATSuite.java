/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import com.ibm.ws.microprofile.config.fat.tests.CDIBrokenInjectionTest;
import com.ibm.ws.microprofile.config.fat.tests.ClassLoaderCacheTest;
import com.ibm.ws.microprofile.config.fat.tests.DynamicSourcesTest;
import com.ibm.ws.microprofile.config.fat.tests.LibertySpecificConfigTests;
import com.ibm.ws.microprofile.config.fat.tests.OrdinalsForDefaultsTest;
import com.ibm.ws.microprofile.config.fat.tests.SimultaneousRequestsTest;
import com.ibm.ws.microprofile.config.fat.tests.VisibilityTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                OrdinalsForDefaultsTest.class, //FULL
                SimultaneousRequestsTest.class, //FULL
                VisibilityTest.class, //FULL

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
