/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.graphql.fat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;


@RunWith(Suite.class)
@SuiteClasses({
                BasicMutationTest.class,
                BasicQueryTest.class,
                BasicQueryWithConfigTest.class,
                DefaultValueTest.class,
                //DeprecationTest.class, // Deprecation behavior was removed from the 1.0 spec
                GraphQLInterfaceTest.class,
                IfaceTest.class,
                IgnoreTest.class,
                InputFieldsTest.class,
                JarInWarTest.class,
                MetricsTest.class,
                OutputFieldsTest.class,
                RolesAuthTest.class,
                TypesTest.class,
                UITest.class,
                VoidQueryTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                                             .andWith(new FeatureReplacementAction("mpConfig-1.4", "mpConfig-2.0")
                                                      .addFeature("mpMetrics-3.0").removeFeature("mpMetrics-2.3")
                                                      .addFeature("mpRestClient-2.0").removeFeature("mpRestClient-1.4")
                                                      .withID("mp4.0").fullFATOnly())
                                             .andWith(new JakartaEE9Action()
                                                      .removeFeatures(setOf("mpConfig-1.4", "mpConfig-2.0")).addFeature("mpConfig-3.0")
                                                      .removeFeatures(setOf("mpMetrics-3.0", "mpMetrics-2.3")).addFeature("mpMetrics-4.0")
                                                      .removeFeatures(setOf("mpRestClient-2.0", "mpRestClient-1.4")).addFeature("mpRestClient-3.0")
                                                      .removeFeature("mpGraphQL-1.0").addFeature("mpGraphQL-2.0")
                                                      .withID(MicroProfileActions.STANDALONE9_ID).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                                             .andWith(new JakartaEE10Action()
                                                      .removeFeatures(setOf("mpConfig-1.4", "mpConfig-2.0")).addFeature("mpConfig-3.0")
                                                      .removeFeatures(setOf("mpMetrics-3.0", "mpMetrics-2.3", "mpMetrics-4.0")).addFeature("mpMetrics-5.0")
                                                      .removeFeatures(setOf("mpRestClient-2.0", "mpRestClient-1.4")).addFeature("mpRestClient-3.0")
                                                      .removeFeature("mpGraphQL-1.0").addFeature("mpGraphQL-2.0").alwaysAddFeature("servlet-6.0")
                                                      .withID(MicroProfileActions.STANDALONE10_ID));

    private static Set<String> setOf(String... strings) {
        // basically does what Java 11's Set.of(...) does
        Set<String> set = new HashSet<>();
        for(String s : strings) {
            set.add(s);
        }
        return set;
    }
    public static void addSmallRyeGraphQLClientLibraries(WebArchive webArchive) {
        File libs = new File("publish/shared/resources/smallryeGraphQLClient/");
        webArchive.addAsLibraries(libs.listFiles());
    }
}
