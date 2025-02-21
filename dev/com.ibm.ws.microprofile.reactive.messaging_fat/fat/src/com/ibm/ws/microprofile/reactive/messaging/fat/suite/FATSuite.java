/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import com.ibm.ws.microprofile.reactive.messaging.fat.jsonb.JsonbTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.tests.KafkaTestClientProviderTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.badconfig.KafkaBadConfigTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.nolib.KafkaNoLibTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({
                PlaintextTests.class,
                KafkaTestClientProviderTest.class,
                KafkaNoLibTest.class,
                KafkaBadConfigTest.class,
                JsonbTest.class
})

public class FATSuite extends TestContainerSuite {

}
