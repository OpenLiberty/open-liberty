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
package com.ibm.ws.fat.wc;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.fat.wc.tests.WC5GetContextPath;
import com.ibm.ws.fat.wc.tests.WC5JakartaServletTest;
import com.ibm.ws.fat.wc.tests.WCAddJspFileTest;
import com.ibm.ws.fat.wc.tests.WCApplicationMBeanStatusTest;
import com.ibm.ws.fat.wc.tests.WCContextRootPrecedence;
import com.ibm.ws.fat.wc.tests.WCEncodingTest;
import com.ibm.ws.fat.wc.tests.WCGetMappingSlashStarTest;
import com.ibm.ws.fat.wc.tests.WCGetMappingTest;
import com.ibm.ws.fat.wc.tests.WCPushBuilderTest;
import com.ibm.ws.fat.wc.tests.WCSameSiteCookieAttributeTests;
import com.ibm.ws.fat.wc.tests.WCSendRedirectRelativeURLDefault;
import com.ibm.ws.fat.wc.tests.WCSendRedirectRelativeURLTrue;
import com.ibm.ws.fat.wc.tests.WCServerTest;
import com.ibm.ws.fat.wc.tests.WCServletClarificationTest;
import com.ibm.ws.fat.wc.tests.WCServletContainerInitializerExceptionTest;
import com.ibm.ws.fat.wc.tests.WCServletContainerInitializerFilterServletNameMappingTest;
import com.ibm.ws.fat.wc.tests.WCServletPathForDefaultMappingDefault;
import com.ibm.ws.fat.wc.tests.WCServletPathForDefaultMappingFalse;
import com.ibm.ws.fat.wc.tests.WCTrailersTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

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
                // Shared Servers
                WCPushBuilderTest.class,
                WCServletClarificationTest.class,
                WCContextRootPrecedence.class,
                WCTrailersTest.class,
                // TFB:
                // Locally, WCTrailersTest fails unless I add '-Dglobal.debug.java2.sec=false' to
                // the gradlew 'buildandrun' invocation.
                // And, when WCTrailersTest fails, it causes most of the tests to fail with errors.
                // I'm still determining if this is purely a local problem.
                //              WCPushBuilderSecurityTest.class,
                WCAddJspFileTest.class,
                WCServletContainerInitializerFilterServletNameMappingTest.class,
                WCApplicationMBeanStatusTest.class,
                // @Server Annotations
                WCEncodingTest.class,
                WCServerTest.class,
                WC5JakartaServletTest.class,
                WCGetMappingTest.class,
                WCServletContainerInitializerExceptionTest.class,
                WCSameSiteCookieAttributeTests.class,
                WCServletPathForDefaultMappingDefault.class,
                WCServletPathForDefaultMappingFalse.class,
                WCGetMappingSlashStarTest.class,
                WCSendRedirectRelativeURLTrue.class,
                WCSendRedirectRelativeURLDefault.class,
                WC5GetContextPath.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
