/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
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

    public static void addSmallRyeGraphQLClientLibraries(WebArchive webArchive) {
        File libs = new File("publish/shared/resources/smallryeGraphQLClient/");
        webArchive.addAsLibraries(libs.listFiles());
    }

    /**
     * Return a rule to repeat tests for MicroProfile 7.0, 6.1, 5.0, 4.0 and 3.3.
     * This translates to GraphQL 2.0 and 1.0 respectively
     *
     * Covers the latest version plus most of the other major versions.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS, TestMode.FULL,
                                          MicroProfileActions.MP70_EE10, //Graph QL 2.0
                                          MicroProfileActions.MP70_EE11, //Graph QL 2.0
                                          MicroProfileActions.MP61, //Graph QL 2.0
                                          MicroProfileActions.MP50, //Graph QL 2.0
                                          MicroProfileActions.MP40, //Graph QL 1.0
                                          MicroProfileActions.MP33); //Graph QL 1.0
}
