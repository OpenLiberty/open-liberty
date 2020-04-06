/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.fat.wc.tests.WCDummyTest;
import com.ibm.ws.fat.wc.tests.WCServerTest;
import com.ibm.ws.fat.wc.tests.AsyncReadListenerHttpUnit;
import com.ibm.ws.fat.wc.tests.AsyncWriteListenerHttpUnit;
import com.ibm.ws.fat.wc.tests.UpgradeWriteListenerHttpUnit;
import com.ibm.ws.fat.wc.tests.UpgradeReadListenerHttpUnit;
import com.ibm.ws.fat.wc.tests.UpgradeReadWriteTimeoutHttpUnit;
import com.ibm.ws.fat.wc.tests.VHServerHttpUnit;
import com.ibm.ws.fat.wc.tests.WCServerHttpUnit;
import com.ibm.ws.fat.wc.tests.JSPServerHttpUnit;
import com.ibm.ws.fat.wc.tests.DefaultErrorPageTest;
import com.ibm.ws.fat.wc.tests.HttpSessionAttListenerHttpUnit;
import com.ibm.ws.fat.wc.tests.CDITests;
import com.ibm.ws.fat.wc.tests.CDIUpgradeHandlerTest;
import com.ibm.ws.fat.wc.tests.CDIServletInterceptorTest;
import com.ibm.ws.fat.wc.tests.CDIBeanInterceptorServletTest;
import com.ibm.ws.fat.wc.tests.CDIListenersTest;
import com.ibm.ws.fat.wc.tests.CDINoInjectionTest;
import com.ibm.ws.fat.wc.tests.CDIServletFilterListenerDynamicTest;
import com.ibm.ws.fat.wc.tests.CDIServletFilterListenerTest;
import com.ibm.ws.fat.wc.tests.AsyncServletTest;
import com.ibm.ws.fat.wc.tests.FormLoginReadListenerTest;
import com.ibm.ws.fat.wc.tests.NBMultiReadTest;

/**
 * Servlet 3.1 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish full mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use lite mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only lite mode tests are run. To also run
 * full mode tests a property must be specified to ant:
 *
 * Select the target build file (usually "build-test.xml").
 * Right click and chose "Run As>Ant Build…". Add
 * "fat.test.mode=full" to the properties tab, then launch the
 * build.
 *
 * Alternatively, for a comand line launch, add "-Dfat.test.mode=full".
 *
 * For additional information see:
 *
 * http://was.pok.ibm.com/xwiki/bin/view/Liberty/Test-FAT
 */
@RunWith(Suite.class)
@SuiteClasses({
               WCDummyTest.class,
               WCServerTest.class,
               AsyncReadListenerHttpUnit.class,
               AsyncWriteListenerHttpUnit.class,
               UpgradeWriteListenerHttpUnit.class,
               UpgradeReadListenerHttpUnit.class,
               UpgradeReadWriteTimeoutHttpUnit.class,
               VHServerHttpUnit.class,
               WCServerHttpUnit.class,
               JSPServerHttpUnit.class,
               DefaultErrorPageTest.class,
               HttpSessionAttListenerHttpUnit.class,
               CDITests.class,
               CDIUpgradeHandlerTest.class,
               CDIServletInterceptorTest.class,
               CDIBeanInterceptorServletTest.class,
               CDIListenersTest.class,
               CDINoInjectionTest.class,
               CDIServletFilterListenerDynamicTest.class,
               CDIServletFilterListenerTest.class,
               AsyncServletTest.class,
               FormLoginReadListenerTest.class,
               NBMultiReadTest.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
