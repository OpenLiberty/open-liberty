/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
        "publish/bundles/cdi.spi.extension.jar", 
        "publish/bundles/cdi.spi.misplaced.jar" 
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
    public static void setUp() {
        FatLogHandler.generateHelpFile();
        //Transform the bundles for EE9
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            for (String path : PATHS_TO_BUNDLES) {
                //We transform into a new bundle because on Windows the transformer will be stopped by 
                //a filelock if it tries to modify a userbundle jar even if we've uninstalled the userbundle.
                //the bundles must not be installed before the EE9 run. 
                //(this is done during the EE9 run rather than the safer choice of transforming first so it can
                //be installed at any time to avoid running the transformer if it is not needed)
                JakartaEE9Action.transformApp(Paths.get(path), Paths.get(path.replace(".jar", "-jakarta.jar")));
            }
        }
    }

}
