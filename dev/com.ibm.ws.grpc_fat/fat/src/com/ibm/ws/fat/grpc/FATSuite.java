/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
                ServiceSupportTests.class, // always keep this on top
                ClientConfigTests.class,
                ClientHeaderPropagationTests.class,
                HelloWorldCDITests.class,
                HelloWorldTest.class,
                HelloWorldEarTest.class,
                HelloWorldTlsTest.class,
                HelloWorldThirdPartyApiTest.class,
                SecureHelloWorldTest.class,
                GrpcMetricsTest.class,
                ServiceConfigTests.class,
                ServiceInterceptorTests.class,
                StoreServicesRESTClientTests.class,
                StoreServicesSecurityTests.class,
                // leave out for now, to avoid intermittent build breaks StreamingTests.class,
                ClientInterceptorTests.class,
                StoreProducerServletClientTests.class,
                StoreConsumerServletClientTests.class
})

public class FATSuite {

    private static final Class<?> c = FATSuite.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("servlet-4.0").alwaysAddFeature("servlet-5.0"));

}
