/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.testcontainers.example;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                BasicTest.class, //LITE
                ContainersTest.class, //LITE
                DatabaseRotationTest.class, //LITE
                DockerfileTest.class, //FULL
                SyntheticImageTest.class //FULL
})
/**
 * Example FATSuite class to show how to setup suite level testcontainers and properties.
 *
 * The suite class MUST extend the TestContainerSuite class.
 * The TestContainerSuite will do important initialization and verification steps
 */
public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());

    private static final DockerImageName PostgreSQLImage = DockerImageName.parse("public.ecr.aws/docker/library/postgres:17-alpine")
                    .asCompatibleSubstituteFor("postgres");

    /*
     * If you want to use the same container for the entire test suite you can
     * declare it here. Using the @ClassRule annotation will start the container
     * prior to any test classes running.
     *
     * In this example suite I am going to use a different container for each example.
     */
    @ClassRule
    public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>(PostgreSQLImage);

}
