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

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());

}
