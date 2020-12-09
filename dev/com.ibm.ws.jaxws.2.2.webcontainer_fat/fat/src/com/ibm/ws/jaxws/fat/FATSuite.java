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
package com.ibm.ws.jaxws.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

/*
 * TODO: Lite Mode
 */
@RunWith(Suite.class)
@SuiteClasses({
//                WebServiceInWebXMLTest.class,
//                WebServiceContextTest.class,
//                HandlerChainTest.class,
//                WebServiceRefTest.class,
//                CatalogFacilityTest.class,
//                WebServiceRefFeaturesTest.class,
                ServerSideStubClientTest.class,
//                PureCXFTest.class,
//                WsBndServiceRefOverrideTest.class,
//                WsBndEndpointOverrideTest.class,
//                CXFJMXSupportTest.class,
//                WebServiceMonitorTest.class,
//                HttpConduitPropertiesTest.class,
//                EJBServiceRefBndTest.class,
//                PortComponentRefTest.class,
//                EndpointPropertiesTest.class,
//                BindingTypeWsdlMismatchTest.class,
//                MTOMTest.class,
//                HandlerChainWithWebServiceClientTest.class,
//                VirtualHostTest.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES()).andWith(new JakartaEE9Action());
}
