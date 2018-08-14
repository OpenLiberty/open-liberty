/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs20.cdi12.fat.test.Basic12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.BeanValidation12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.Complex12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.DisableTest;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LifeCycle12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LifeCycleMismatch12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.LoadOnStartup12Test;
import com.ibm.ws.jaxrs20.cdi12.fat.test.ResourceInfoAtStartupTest;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;

@RunWith(Suite.class)
@SuiteClasses({
                DisableTest.class,
                Basic12Test.class,
                BeanValidation12Test.class,
                LifeCycle12Test.class,
                LifeCycleMismatch12Test.class,
                LoadOnStartup12Test.class,
                Complex12Test.class,
                ResourceInfoAtStartupTest.class
})
public class FATSuiteCDI12WithJava7 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();
}
