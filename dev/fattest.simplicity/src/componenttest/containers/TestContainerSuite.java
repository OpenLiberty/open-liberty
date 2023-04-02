/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package componenttest.containers;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.log.Log;

public class TestContainerSuite {

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {

        /*
         * THIS METHOD CALL IS REQUIRED TO USE TESTCONTAINERS PLEASE READ:
         *
         * Testcontainers caches data in a properties file located at $HOME/.testcontainers.properties
         * The ExternalTestServiceDockerClientStrategy.setup* methods will clear and reset the values in this property file.
         *
         * By default, testcontainers will attempt to run against a local docker instance and pull from DockerHub.
         * If you want testcontainers to run against a remote docker host to mirror the behavior of an RTC build
         * Then, set property: -Dfat.test.use.remote.docker=true
         * This will only work if you are on the IBM network.
         *
         * We will set the following properties:
         * 1. docker.client.strategy:
         * Default: [Depends on local OS]
         * Custom : componenttest.containers.ExternalTestServiceDockerClientStrategy
         * Purpose: This is the strategy testcontainers uses to locate and run against a remote docker instance.
         *
         * 2. image.substitutor:
         * Default: [none]
         * Custom : componenttest.containers.ArtifactoryImageNameSubstitutor
         * Purpose: This defines a strategy for substituting image names.
         * This is so that we can use a private docker repository to cache docker images
         * to avoid the docker pull limits.
         * Example: foo/bar:1.0 it will get changed to wasliberty-docker-remote.artifactory.swg-devops.com/foo/bar:1.0
         */
        @Override
        protected void before() throws Throwable {
            Log.info(TestContainerSuite.class, "before", "Setting up testcontainers");
            ExternalTestServiceDockerClientStrategy.setupTestcontainers();
        }

        @Override
        protected void after() {
            Log.info(TestContainerSuite.class, "after", "Assert all container images have been declared");
            ImageVerifier.assertImages();
        }
    };
}
