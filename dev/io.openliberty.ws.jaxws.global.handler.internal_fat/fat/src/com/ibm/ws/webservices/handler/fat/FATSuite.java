/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.handler.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of a few fast example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should only be mainline test cases that complete
 * in a combined total of 5 minutes or less.
 */
@SuiteClasses({
               AddNumbersTest.class,           
               EJBHandlerTest.class,
               EJBServiceRefBndTest.class,
               HandlerChainTest.class,
               HandlerRuleTest.class,
               TransportSecurityUsingDispatchClientCertTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                                             .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                                             .andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jaxwsTest-2.2").addFeature("xmlWSTest-3.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                                             .andWith(FeatureReplacementAction.EE10_FEATURES().removeFeature("jaxwsTest-2.2").removeFeature("xmlWSTest-3.0").addFeature("xmlWSTest-4.0"));
}