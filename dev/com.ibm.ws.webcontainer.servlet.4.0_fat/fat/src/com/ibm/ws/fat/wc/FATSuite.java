/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.fat.wc.tests.WC400BadRequestTest;
import com.ibm.ws.fat.wc.tests.WC500BadRequestDefaultTest;
import com.ibm.ws.fat.wc.tests.WC5GetContextPath;
import com.ibm.ws.fat.wc.tests.WC5JakartaServletTest;
import com.ibm.ws.fat.wc.tests.WCAddJspFileTest;
import com.ibm.ws.fat.wc.tests.WCApplicationMBeanStatusTest;
import com.ibm.ws.fat.wc.tests.WCContextRootPrecedence;
import com.ibm.ws.fat.wc.tests.WCEncodingTest;
import com.ibm.ws.fat.wc.tests.WCFileUpLoadFileCountMaxPropertyTest;
import com.ibm.ws.fat.wc.tests.WCFileUpLoadFileCountMaxTest;
import com.ibm.ws.fat.wc.tests.WCGetMappingSlashStarTest;
import com.ibm.ws.fat.wc.tests.WCGetMappingTest;
import com.ibm.ws.fat.wc.tests.WCPushBuilderTest;
import com.ibm.ws.fat.wc.tests.WCResponseHeadersTest;
import com.ibm.ws.fat.wc.tests.WCSCIHandlesTypesTest;
import com.ibm.ws.fat.wc.tests.WCSameSiteCookieAttributeSecurityTest;
import com.ibm.ws.fat.wc.tests.WCSameSiteCookieAttributeTests;
import com.ibm.ws.fat.wc.tests.WCSendRedirectRelativeURLDefault;
import com.ibm.ws.fat.wc.tests.WCSendRedirectRelativeURLTrue;
import com.ibm.ws.fat.wc.tests.WCServerMiscTest;
import com.ibm.ws.fat.wc.tests.WCServerPropertyTest;
import com.ibm.ws.fat.wc.tests.WCServerTest;
import com.ibm.ws.fat.wc.tests.WCServletClarificationTest;
import com.ibm.ws.fat.wc.tests.WCServletContainerInitializerExceptionTest;
import com.ibm.ws.fat.wc.tests.WCServletContainerInitializerFilterServletNameMappingTest;
import com.ibm.ws.fat.wc.tests.WCServletContextUnsupportedOperationExceptionTest;
import com.ibm.ws.fat.wc.tests.WCServletPathForDefaultMappingDefault;
import com.ibm.ws.fat.wc.tests.WCServletPathForDefaultMappingFalse;
import com.ibm.ws.fat.wc.tests.WCTestEncodedX590;
import com.ibm.ws.fat.wc.tests.WCTrailersTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

/**
 * Servlet 4.0 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish FULL mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use LITE mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only LITE mode tests are run. To also run
 * full mode tests a property must be specified:
 *
 * -Dfat.test.mode=FULL.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                // WCPushBuilderSecurityTest.class,
                WC500BadRequestDefaultTest.class,
                WC400BadRequestTest.class,
                WCApplicationMBeanStatusTest.class,
                WCContextRootPrecedence.class,
                WCPushBuilderTest.class,
                WCServletContainerInitializerFilterServletNameMappingTest.class,
                WCServletClarificationTest.class,
                WCAddJspFileTest.class,
                WCTrailersTest.class,
                WCEncodingTest.class,
                WCFileUpLoadFileCountMaxPropertyTest.class,
                WCFileUpLoadFileCountMaxTest.class,
                WCServerTest.class,
                WC5JakartaServletTest.class,
                WCGetMappingTest.class,
                WCServletContainerInitializerExceptionTest.class,
                WCSameSiteCookieAttributeTests.class,
                WCSameSiteCookieAttributeSecurityTest.class,
                WCServletPathForDefaultMappingDefault.class,
                WCServletPathForDefaultMappingFalse.class,
                WCGetMappingSlashStarTest.class,
                WCSendRedirectRelativeURLTrue.class,
                WCSendRedirectRelativeURLDefault.class,
                WC5GetContextPath.class,
                WCSCIHandlesTypesTest.class,
                WCResponseHeadersTest.class,
                WCServerMiscTest.class,
                WCServerPropertyTest.class,
                WCTestEncodedX590.class,
                WCServletContextUnsupportedOperationExceptionTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly()).andWith(FeatureReplacementAction.EE10_FEATURES());
        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES());
        }
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
