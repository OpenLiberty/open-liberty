/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
               TransportSecurityUsingDispatchClientCertTest.class,
               EJBHandlerTest.class,
               EJBServiceRefBndTest.class,
               AddNumbersTest.class,
               HandlerChainTest.class,
               JAXRSDynamicHandlerTest.class,
               HandlerRuleTest.class,
               JAXRSMessageContextAPITest.class,
               CDIRolesAllowedTest.class,
               //NoInterfaceEJBTest.class, // some unexpected migration (from CL to OL) failures
               //OneInterfaceEJBTest.class,// ditto
               JAXRSClientHandlerTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                                             .andWith(FeatureReplacementAction.EE8_FEATURES())
                                             .andWith(FeatureReplacementAction.EE9_FEATURES());
}