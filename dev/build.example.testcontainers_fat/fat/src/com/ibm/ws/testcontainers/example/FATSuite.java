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
package com.ibm.ws.testcontainers.example;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ContainersTest.class,
                DatabaseRotationTest.class,
                DockerfileTest.class
})
/**
 * Example FATSuite class to show how to setup suite level testcontainers and properties
 */
public class FATSuite {

    /*
     * TestContainers uses a properties file located at ~/.testcontainers.properties
     * This method call clears and set's the values in this property file.
     *
     * Unless otherwise specified TestContainers will attempt to run against a local
     * docker instance, and pull from DockerHub. If you set the property:
     * -Dfat.test.use.remote.docker=true
     * We will change the properties below. This only works if you are on the IBM network.
     *
     * We use two properties in this file:
     * 1. docker.client.strategy:
     * Default: org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
     * Custom: componenttest.containers.ExternalTestServiceDockerClientStrategy
     * Purpose: This is the strategy TestContainers uses to locate and
     * run against a docker instance.
     * 2. image.substitutor:
     * Default: [none]
     * Custom: componenttest.containers.ArtifactoryImageNameSubstitutor
     * Purpose: This defines a strategy for substituting image names.
     * This is so that we can use a private docker repository to cache docker images
     * to avoid the docker pull limits.
     * If a TestContainer uses foo/bar:1.0 it will get changed to
     * wasliberty-docker-remote.artifactory.swg-devops.com/foo/bar:1.0
     */
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    /*
     * If you want to use the same container for the entire test suite you can
     * declare it here. Using the @ClassRule annotation will start the container
     * prior to any test classes running.
     *
     * In this example suite I am going to use a different container for each example.
     */
//    @ClassRule
//    public static PostgreSQLContainer container = new PostgreSQLContainer("postgres:9.6.12");

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());

}
