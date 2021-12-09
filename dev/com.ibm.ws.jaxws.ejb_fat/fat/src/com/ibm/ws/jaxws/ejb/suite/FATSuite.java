/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb.suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxws.ejb.fat.EJBHandlerTest;
import com.ibm.ws.jaxws.ejb.fat.EJBInWarServiceTest;
import com.ibm.ws.jaxws.ejb.fat.EJBJndiTest;
import com.ibm.ws.jaxws.ejb.fat.EJBWSBasicTest;
import com.ibm.ws.jaxws.ejb.fat.EJBWSContextTest;
import com.ibm.ws.jaxws.ejb.fat.EJBWSInterceptorTest;
import com.ibm.ws.jaxws.ejb.fat.EJBWSLifeCycleTest;
import com.ibm.ws.jaxws.ejb.fat.EJBWSProviderTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
                EJBInWarServiceTest.class,
                EJBWSBasicTest.class,
                EJBWSProviderTest.class,
                EJBHandlerTest.class,
                EJBWSContextTest.class,
                EJBWSLifeCycleTest.class,
                EJBJndiTest.class,
                EJBWSInterceptorTest.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction()).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().removeFeature("jaxws-2.2").addFeature("jaxws-2.3").withID("jaxws-2.3")).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jaxws-2.3"));
}
