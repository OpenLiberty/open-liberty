/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.JakartaEE9Action;

import com.ibm.ws.messaging.JMS20.fat.ContextInject.JMSContextInjectTest;
import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnsharedTest;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118076;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118077;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSEjbJarXmlMdbTest;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSRedeliveryTest_120846;
import com.ibm.ws.messaging.JMS20.fat.JMSDCFTest.JMSDCFVarTest;
import com.ibm.ws.messaging.JMS20.fat.JMSDCFTest.JMSDCFTest;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118071;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118073;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducer_Test118073;
import com.ibm.ws.messaging.JMS20.fat.JMSMBeanTest.JMSMBeanTest;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129626;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129626;

@RunWith(Suite.class)
@SuiteClasses({

        DummyTest.class,
        LiteBucketSet1Test.class,

        // Two test methods temporarily disabled for jakarta due to injection exceptions.
        // See the LiteBucketSet2Test source for more information.
        LiteBucketSet2Test.class,

        // Fully disabled for jakarta.  j2ee-management is not supported by jakarta, and
        // is not compatible with the jakarta features used by the test class.
        JMSMBeanTest.class,

        JMSProducerTest_118071.class, //full
        JMSProducerTest_118073.class, //full
        SharedSubscriptionTest_129623.class,

        JMSConsumerTest_118076.class, //full
        JMSConsumerTest_118077.class, //full
        JMSRedeliveryTest_120846.class,

        SharedSubscriptionWithMsgSelTest_129623.class,
        SharedSubscriptionWithMsgSelTest_129626.class, //full 2nd
        SharedSubscriptionTest_129626.class, //full 2nd
        JMSProducer_Test118073.class, //full

        DurableUnsharedTest.class,

        // Temporarily disabled for jakarta due to injection exceptions.
        // See the JMSContextInjectTest source for more information.
        JMSContextInjectTest.class, //full

// xx JMSDCFTest.class,
        JMSDCFVarTest.class //full 2nd

// xx JMSEjbJarXmlMdbTest.class, // MDBMDB
})
public class FATSuite {
    @ClassRule
    public static RepeatTests repeater = RepeatTests
        .withoutModification()
        .andWith( new JakartaEE9Action() );
}
