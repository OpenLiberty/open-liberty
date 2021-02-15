/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import static componenttest.rules.repeater.MicroProfileActions.MP20;
import static componenttest.rules.repeater.MicroProfileActions.MP40;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.reactive.messaging.fat.jsonb.JsonbTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.tests.KafkaTestClientProviderTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.badconfig.KafkaBadConfigTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.nolib.KafkaNoLibTest;
import com.ibm.ws.microprofile.reactive.messaging.fat.loginModuleClassloading.LoginModuleClassloadingTest;

import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                PlaintextTests.class,
                TlsTests.class,
                SaslPlainTests.class,
                KafkaTestClientProviderTest.class,
                LoginModuleClassloadingTest.class,
                KafkaNoLibTest.class,
                KafkaBadConfigTest.class,
                JsonbTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(null, MP20, MP40);

}
