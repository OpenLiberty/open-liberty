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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


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
}
