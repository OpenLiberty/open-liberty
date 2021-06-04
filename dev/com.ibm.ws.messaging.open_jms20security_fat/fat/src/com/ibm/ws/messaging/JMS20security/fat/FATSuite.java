/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20security.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20.fat.ContextInject.JMSContextInjectTest;
import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnshared;
import com.ibm.ws.messaging.JMS20.fat.MDB.JMSMDBTest;
import com.ibm.ws.messaging.JMS20.fat.TemporaryQueue.JMSContextTest_118066;
import com.ibm.ws.messaging.JMS20.fat.TemporaryQueue.JMSContextTest_118068;
import com.ibm.ws.messaging.JMS20.fat.Transaction.JMSContextTest_118065;
import com.ibm.ws.messaging.JMS20security.fat.DCFTest.JMSDefaultConnectionFactorySecurityTest;
import com.ibm.ws.messaging.JMS20security.fat.JMSConsumerTest.JMSConsumerTest;
import com.ibm.ws.messaging.JMS20security.fat.JMSContextTest.JMSContextTest;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DummyTest.class,
                JMSContextTest.class,
                JMSConsumerTest.class,
                JMSContextTest_118066.class,
                JMSContextTest_118068.class,
                JMSContextTest_118065.class,
                DurableUnshared.class,
                JMSContextInjectTest.class,
                JMSDefaultConnectionFactorySecurityTest.class,
                JMSMDBTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                                             .andWith( new JakartaEE9Action() );
}
