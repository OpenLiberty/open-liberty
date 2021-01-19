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
package com.ibm.ws.jsp23.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsp23.fat.tests.JSP23JSP22ServerTest;
import com.ibm.ws.jsp23.fat.tests.JSPCdiTest;
import com.ibm.ws.jsp23.fat.tests.JSPJava8Test;
import com.ibm.ws.jsp23.fat.tests.JSPPrepareJSPThreadCountDefaultValueTests;
import com.ibm.ws.jsp23.fat.tests.JSPPrepareJSPThreadCountNonDefaultValueTests;
import com.ibm.ws.jsp23.fat.tests.JSPTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * JSP 2.3 Tests
 *
 * The tests for both features should be included in this test component.
 */
@RunWith(Suite.class)
@SuiteClasses({
                JSPTests.class,
                JSPJava8Test.class,
                JSPCdiTest.class,
                JSP23JSP22ServerTest.class,
                JSPPrepareJSPThreadCountNonDefaultValueTests.class,
                JSPPrepareJSPThreadCountDefaultValueTests.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    /**
     * Run the tests again with the cdi-2.0 feature. Tests should be skipped where appropriate
     * using @SkipForRepeat("CDI-2.0").
     */
    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new EmptyAction().fullFATOnly())
                    .andWith(new FeatureReplacementAction("cdi-1.2", "cdi-2.0")
                                    .withID("CDI-2.0")
                                    .forceAddFeatures(false)
                                    .fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
