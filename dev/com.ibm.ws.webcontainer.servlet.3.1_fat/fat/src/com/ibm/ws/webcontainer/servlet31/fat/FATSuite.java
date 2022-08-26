/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.webcontainer.servlet31.fat.tests.AsyncReadListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.AsyncWriteListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIBeanInterceptorServletTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIListenersTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDINoInjectionTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIServletFilterListenerDynamicTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIServletFilterListenerTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIServletInterceptorTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDITests;
import com.ibm.ws.webcontainer.servlet31.fat.tests.CDIUpgradeHandlerTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.FormLoginReadListenerTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.HttpSessionAttListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.JSPServerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.NBMultiReadTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeReadListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeReadWriteTimeoutHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeWriteListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.VHServerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.WCServerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.WCServerTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.WCServletContextUnsupportedOperationExceptionTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

/**
 * Servlet 3.1 Tests with repeat for Servlet 4.0
 */
@RunWith(Suite.class)
@SuiteClasses({
                WCServerTest.class,
                AsyncReadListenerHttpUnit.class,
                AsyncWriteListenerHttpUnit.class,
                UpgradeWriteListenerHttpUnit.class,
                UpgradeReadListenerHttpUnit.class,
                UpgradeReadWriteTimeoutHttpUnit.class,
                VHServerHttpUnit.class,
                WCServerHttpUnit.class,
                JSPServerHttpUnit.class,
                HttpSessionAttListenerHttpUnit.class,
                CDITests.class,
                CDIUpgradeHandlerTest.class,
                CDIServletInterceptorTest.class,
                CDIBeanInterceptorServletTest.class,
                CDIListenersTest.class,
                CDINoInjectionTest.class,
                CDIServletFilterListenerDynamicTest.class,
                CDIServletFilterListenerTest.class,
                FormLoginReadListenerTest.class,
                NBMultiReadTest.class,
                WCServletContextUnsupportedOperationExceptionTest.class
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
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE10_FEATURES());
        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES());
        }
    }

}
