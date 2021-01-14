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

import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi12.fat.tests.AppExtensionTest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionSPIConstructorExceptionTest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionSPITest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionTest;
import com.ibm.ws.cdi12.fat.tests.DynamicBeanExtensionTest;
import com.ibm.ws.cdi12.fat.tests.ObserverTest;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;


/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AppExtensionTest.class,
                CDI12ExtensionTest.class,
                CDI12ExtensionSPIConstructorExceptionTest.class,
                CDI12ExtensionSPITest.class,
                DynamicBeanExtensionTest.class,
                ObserverTest.class
})

public class FATSuite {

    private static final String[] PATHS_TO_BUNDLES = {
        "publish/bundles/cdi.helloworld.extension.jar", 
        "publish/bundles/cdi.spi.constructor.fail.extension.jar", 
        "publish/bundles/cdi.spi.extension.jar" 
    };

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES())
                                        .andWith(FeatureReplacementAction.EE9_FEATURES()
                                        .removeFeature("usr:cdi.helloworld.extension-1.0").removeFeature("usr:cdi.spi.extension-1.0")
                                        .removeFeature("usr:cdi.spi.constructor.fail.extension-1.0").removeFeature("cdi.internals-1.0")
                                        .addFeature("usr:cdi.helloworld.extension-3.0").addFeature("usr:cdi.spi.extension-3.0")
                                        .addFeature("usr:cdi.spi.constructor.fail.extension-3.0").addFeature("cdi.internals-3.0")
                                        );

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    @AfterClass
    public static void transformUserFeatures() {
        //If we've finished EE8 then transform javax to Jakarta in preperation for EE9
        if (RepeatTestFilter.isRepeatActionActive("EE8_FEATURES")) {
            for (String path : PATHS_TO_BUNDLES) {
                JakartaEE9Action.transformApp(Paths.get(path));
            }
        }
    }

}
