/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.el.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.el.fat.tests.EL22OperatorsTest;
import com.ibm.ws.el.fat.tests.EL30CoercionRulesTest;
import com.ibm.ws.el.fat.tests.EL30LambdaExpressionsTest;
import com.ibm.ws.el.fat.tests.EL30ListCollectionObjectOperationsTest;
import com.ibm.ws.el.fat.tests.EL30MethodExpressionInvocationsTest;
import com.ibm.ws.el.fat.tests.EL30MiscTests;
import com.ibm.ws.el.fat.tests.EL30OperatorPrecedenceTest;
import com.ibm.ws.el.fat.tests.EL30OperatorsTest;
import com.ibm.ws.el.fat.tests.EL30ReservedWordsTest;
import com.ibm.ws.el.fat.tests.EL30StaticFieldsAndMethodsTest;
import com.ibm.ws.el.fat.tests.EL30VarargsMethodMatchingTest;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * EL 3.0 Tests
 *
 * The tests for both features should be included in this test component.
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish full mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use lite mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only lite mode tests are run.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                EL30LambdaExpressionsTest.class,
                EL30CoercionRulesTest.class,
                EL30ReservedWordsTest.class,
                EL30ListCollectionObjectOperationsTest.class,
                EL30StaticFieldsAndMethodsTest.class,
                EL30OperatorPrecedenceTest.class,
                EL22OperatorsTest.class,
                EL30OperatorsTest.class,
                EL30MethodExpressionInvocationsTest.class,
                EL30VarargsMethodMatchingTest.class,
                EL30MiscTests.class
})
public class FATSuite {

    // EE10 requires Java 11.
    // EE11 requires Java 17
    // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
