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
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.KafkaTlsContainer;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tls.KafkaTlsTest;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

/**
 * Suite for tests which run against a TLS enabled kafka broker
 */
@RunWith(Suite.class)
@SuiteClasses({ KafkaTlsTest.class,
})
public class TlsTests {

    @ClassRule
    public static KafkaTlsContainer kafkaContainer = new KafkaTlsContainer();

    @BeforeClass
    public static void beforeSuite() throws Exception {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }

}
