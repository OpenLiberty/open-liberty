/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.defaultexceptionmapper_fat;

import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@SuiteClasses(DefaultExceptionMapperTest.class)
@RunWith(Suite.class)
public class FATSuite {

    private static final String TEST_BUNDLE = "publish/files/bundles/test.exceptionmapper.jar";

    private static boolean firstRun = true;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                                             .andWith(FeatureReplacementAction.EE9_FEATURES()
                                                                              .removeFeature("test.exceptionmapper-2.0")
                                                                              .addFeature("test.exceptionmapper-3.0"));

    @BeforeClass
    public static void transformTestBundle() {
        if (firstRun) {
            JakartaEE9Action.transformApp(Paths.get(TEST_BUNDLE),
                                          Paths.get(TEST_BUNDLE.replace(".jar", ".jakarta.jar")));
            firstRun = false;
        }
    }
}
