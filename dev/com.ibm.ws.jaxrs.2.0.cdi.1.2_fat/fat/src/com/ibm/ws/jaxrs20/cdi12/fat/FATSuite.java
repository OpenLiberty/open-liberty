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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
               /**
                *
                * CDI 1.0 test (41)
                * Test 1 @inject if inject correct in Resource for PerRequest/Singleton (OK)
                *
                * Test 2 @Context if inject correct in Resource for PerRequest/Singleton (OK)
                *
                * Test 3 @Inject and @Context if works fine in Provider and Filter for Singleton (OK, OK)
                *
                * Test 4 @Context and @Inject if inject correct in Application for PerRequest/Singleton (NoNeed, Null)
                *
                * Test 5 Scope/Lifecycle test for PerRequest/Singleton (Neal will do it)
                *
                * Test 6 Other functions: BeanValidation and Asyn(No Spec for CDI)
                *
                * Test 7 Singleton with Servlet: if CDI within Servlet and CDI is same instance (OK)
                *
                * Test 8 @Alternative to test if is not a managed bean (OK)
                *
                * Test 9 Singleton Constructor with Parameter(@Context), if report error (OK)
                *
                * Test 10 Disable CDI feature, test if works fine (OK)
                *
                * Test 11 Is Resource and Provider at the same time, check if works fine (OK)
                *
                * Test 12 Resource and provider are both in getClass and getSingleton (OK)
                *
                *
                */

               // About 100 test cases
               AlwaysPassesTest.class,
               FATSuiteCDI12WithJava7.class

// ignore the cdi-1.0 tests, because no need support JavaEE 6 feature
// FATSuiteCDI10WithJava7.class
})
public class FATSuite {}
