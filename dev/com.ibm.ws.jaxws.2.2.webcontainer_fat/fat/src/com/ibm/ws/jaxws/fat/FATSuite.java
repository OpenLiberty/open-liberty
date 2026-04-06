/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/*
 * TODO: Lite Mode
 */
@RunWith(Suite.class)
@SuiteClasses({
                AttachmentPropertiesTest.class,
                AsyncClientConnectionTest.class,
                BindingTypeWsdlMismatchTest.class,
                CatalogFacilityTest.class,
                CXFJMXSupportTest.class,
                EndpointPropertiesTest.class,
                HolderTest.class,
                HttpConduitPropertiesTest.class,
                LoggingTest.class,
                MismatchingSOAPActionMTOMTest.class,
                MTOMTest.class,
                LibertyCXFPositivePropertiesTest.class,
                LibertyCXFNegativePropertiesTest.class,
                PartInfoNamespaceCorrectionTest.class,
                PureCXFTest.class,
                ServerSideStubClientTest.class,
                VirtualHostTest.class,
                WebServiceContextTest.class,
                WebServiceMonitorTest.class,
                WebServiceRefFeaturesTest.class,
                WebServiceRefTest.class,
                SoapEnvelopePrefixTest.class
})
public class FATSuite {

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    @ClassRule
    public static RepeatTests r;

    /*@formatter:off*/
    static {
        if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
            r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                            .conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES()
                                            .removeFeature("jaxwstest-2.2")
                                            .removeFeature("xmlwstest-3.0")
                                            .addFeature("xmlwstest-4.0")
                                            .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES()
                                            .removeFeature("jaxwstest-2.2")
                                            .removeFeature("xmlwstest-3.0")
                                            .addFeature("xmlwstest-4.0"));
        } else {
            r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()
                                            .fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES()
                                            .removeFeature("jaxwstest-2.2")
                                            .addFeature("xmlwstest-3.0")
                                            .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES()
                                            .removeFeature("jaxwstest-2.2")
                                            .removeFeature("xmlwstest-3.0")
                                            .addFeature("xmlwstest-4.0")
                                            .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES()
                                            .removeFeature("jaxwstest-2.2")
                                            .removeFeature("xmlwstest-3.0")
                                            .addFeature("xmlwstest-4.0"));
        }
    }
    /*@formatter:on*/

}
