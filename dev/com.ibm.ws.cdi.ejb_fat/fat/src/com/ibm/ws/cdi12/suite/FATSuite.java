/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi12.fat.tests.AroundConstructBeanTest;
import com.ibm.ws.cdi12.fat.tests.AroundConstructEjbTest;
import com.ibm.ws.cdi12.fat.tests.CDIManagedBeanInterceptorTest;
import com.ibm.ws.cdi12.fat.tests.EJB32Test;
import com.ibm.ws.cdi12.fat.tests.EjbConstructorInjectionTest;
import com.ibm.ws.cdi12.fat.tests.EjbDiscoveryTest;
import com.ibm.ws.cdi12.fat.tests.EjbMiscTest;
import com.ibm.ws.cdi12.fat.tests.EjbTimerTest;
import com.ibm.ws.cdi12.fat.tests.InjectParameterTest;
import com.ibm.ws.cdi12.fat.tests.MultipleNamedEJBTest;
import com.ibm.ws.cdi12.fat.tests.StatefulSessionBeanInjectionTest;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                AroundConstructBeanTest.class,
                AroundConstructEjbTest.class,
                CDIManagedBeanInterceptorTest.class,
                EJB32Test.class,
                EjbConstructorInjectionTest.class,
                EjbDiscoveryTest.class,
                EjbMiscTest.class,
                EjbTimerTest.class,
                InjectParameterTest.class,
                MultipleNamedEJBTest.class,
                StatefulSessionBeanInjectionTest.class,
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
