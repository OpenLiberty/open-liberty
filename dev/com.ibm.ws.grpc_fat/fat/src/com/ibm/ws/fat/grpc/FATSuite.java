/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import org.junit.ClassRule;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ServiceSupportTests.class, // always keep this on top //e9
                ClientConfigTests.class,//e9
                ClientHeaderPropagationTests.class,//e9
                // HelloWorldCDITests.class,
                // HelloWorldTest.class,
                HelloWorldTlsTest.class,//e9
                HelloWorldThirdPartyApiTest.class,//e9
                SecureHelloWorldTest.class, //e9
                // GrpcMetricsTest.class,
                ServiceConfigTests.class, // ee9
                ServiceInterceptorTests.class, // e9
                // StoreServicesRESTClientTests.class,
                // StoreServicesSecurityTests.class,
                // // leave out for now, to avoid intermittent build breaks StreamingTests.class,
                ClientInterceptorTests.class, // ee9
                // StoreProducerServletClientTests.class,
                // StoreConsumerServletClientTests.class
})

public class FATSuite {

    private static final Class<?> c = FATSuite.class;

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().addFeature("servlet-5.0"));
}
