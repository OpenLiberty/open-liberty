/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage50.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests.EL50ArrayCoercionTest;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests.EL50BasicTests;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests.EL50DefaultMethodsTest;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests.EL50LambdaExpressionCoercionTest;
import io.openliberty.org.apache.jasper.expressionLanguage50.fat.tests.EL50MethodReferenceTest;

/**
 * Expression Language 5.0 Tests
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
                EL50DefaultMethodsTest.class,
                EL50MethodReferenceTest.class,
                EL50LambdaExpressionCoercionTest.class,
                EL50ArrayCoercionTest.class,
                EL50BasicTests.class,

})
public class FATSuite {

    // EE11 requires Java 17
    // If we only specify EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running on Java 11, have EE10 be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
