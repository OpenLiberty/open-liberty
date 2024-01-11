/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.pages31.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.pages31.fat.tests.MiscTests;
import io.openliberty.pages31.fat.tests.TagsTestsEE10;
import io.openliberty.pages31.fat.tests.UnknownELIdentifiersTest;

/**
 * Pages 3.1 Tests
 *
 * The tests for both features should be included in this test component.
 */
@RunWith(Suite.class)
@SuiteClasses({
                TagsTestsEE10.class,
                UnknownELIdentifiersTest.class,
                MiscTests.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

}
