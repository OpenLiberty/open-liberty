/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

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
                NBMultiReadTest.class 
})
public class FATSuite {
    
    static Set<String> removeFeatures = new HashSet<>(Arrays.asList("servlet-3.1", "cdi-1.2", "jsp-2.2", "javaee-7.0"));
    static Set<String> addFeatures = new HashSet<>(Arrays.asList("servlet-4.0", "jsp-2.3", "cdi-2.0", "javaee-8.0"));

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    .andWith(new FeatureReplacementAction(removeFeatures, addFeatures)
                                    .withID("SERVLET-4.0")
                                    .forceAddFeatures(false)
                                    .withMinJavaLevel(8))
                    .andWith(new JakartaEE9Action());

}
