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
package com.ibm.ws.microprofile.graphql.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
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
    public static RepeatTests r = RepeatTests.withoutModification()
                                             .andWith(new FeatureReplacementAction("mpConfig-1.4", "mpConfig-2.0")
                                                      .addFeature("mpMetrics-3.0").removeFeature("mpMetrics-2.3")
                                                      .addFeature("mpRestClient-2.0").removeFeature("mpRestClient-1.4")
                                                      .withID("mp4.0"));

    public static void addSmallRyeGraphQLClientLibraries(WebArchive webArchive) {
        File libs = new File("publish/shared/resources/smallryeGraphQLClient/");
        webArchive.addAsLibraries(libs.listFiles());
    }
}
