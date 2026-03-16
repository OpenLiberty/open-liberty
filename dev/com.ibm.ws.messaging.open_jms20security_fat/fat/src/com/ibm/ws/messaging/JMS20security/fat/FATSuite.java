/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20security.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnshared;
import com.ibm.ws.messaging.JMS20.fat.MDB.JMSMDBTest;
import com.ibm.ws.messaging.JMS20security.fat.DCFTest.JMSDefaultConnectionFactorySecurityTest;
import com.ibm.ws.messaging.JMS20security.fat.JMSConsumerTest.JMSConsumerTest;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DummyTest.class,
                JMSConsumerTest.class,
                DurableUnshared.class,
                JMSDefaultConnectionFactorySecurityTest.class,
                JMSMDBTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES())
                                             .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                                             .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly())
                                             .andWith(FeatureReplacementAction.EE11_FEATURES().fullFATOnly());
}
