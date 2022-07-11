/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs20.cdi12.fat.test.Basic12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.BeanValidation12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.CDIInjectIntoAppTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.Complex12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.ContextAndClientTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.ContextandCDI12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.DependentIntoJaxTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.DisableTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.InterceptorTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LifeCycle12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LifeCycleMismatch12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LoadOnStartup12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.ResourceInfoAtStartupTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
               AlwaysPassesTest.class,
               Basic12Test.class,             // Skip for JakartaEE9/EE10
               BeanValidation12Test.class, 
               CDIInjectIntoAppTest.class,
               Complex12Test.class,           // Skip for JakartaEE9/EE10
               ContextAndClientTest.class,
               ContextandCDI12Test.class,     // Skip for JakartaEE9/EE10
               DependentIntoJaxTest.class,    // Skip for JakartaEE9/EE10
               DisableTest.class,             // Skip for JakartaEE9/EE10
               InterceptorTest.class,
               LifeCycle12Test.class,         // Skip for JakartaEE9/EE10
               LifeCycleMismatch12Test.class, // Skip for JakartaEE9/EE10
               LoadOnStartup12Test.class,
               ResourceInfoAtStartupTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1"))
                    .andWith(new JakartaEE9Action())
                    .andWith(new JakartaEE10Action());
}
