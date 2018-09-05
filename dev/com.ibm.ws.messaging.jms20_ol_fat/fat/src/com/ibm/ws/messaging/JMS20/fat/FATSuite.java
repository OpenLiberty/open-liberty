/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.messaging.JMS20.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20.fat.ContextInject.JMSContextInjectTest;
import com.ibm.ws.messaging.JMS20.fat.DCFTest.JMSDefaultConnFactoryVariationTest;
import com.ibm.ws.messaging.JMS20.fat.DCFTest.JMSDefaultConnectionFactoryTest;
import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnsharedTest;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumer.topic.JMSContextTest_118077;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118076;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118077;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118058;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118061;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118062;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118067;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118070;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSContextTest_118075;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSEjbJarXmlMdbTest;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSRedeliveryTest_120846;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118071;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118073;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducer_Test118073;
import com.ibm.ws.messaging.JMS20.fat.JmsMBeanTest.JmsMBeanTest;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129626;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129626;
import com.ibm.ws.messaging.JMS20.fat.TemporaryQueue.JMSContextTest_118066;
import com.ibm.ws.messaging.JMS20.fat.TemporaryQueue.JMSContextTest_118068;
import com.ibm.ws.messaging.JMS20.fat.Transaction.JMSContextTest_118065;

@RunWith(Suite.class)
@SuiteClasses({

               DummyTest.class,
               LiteBucketSet1Test.class,
               LiteBucketSet2Test.class,
               LiteBucketSet3Test.class,
               JmsMBeanTest.class,

               JMSContextTest_118058.class, //full

               JMSContextTest_118061.class, //full
               JMSContextTest_118062.class, //full
               JMSProducerTest_118071.class, //full
               JMSProducerTest_118073.class, //full
               SharedSubscriptionTest_129623.class,

               JMSContextTest_118067.class, //full
               JMSContextTest_118070.class, //full
               JMSConsumerTest_118077.class, //full
               JMSConsumerTest_118076.class, //full
               JMSRedeliveryTest_120846.class,
               JMSContextTest_118075.class, //full

               JMSContextTest_118065.class, //full
               JMSContextTest_118066.class, //full
               JMSContextTest_118068.class, //full

               SharedSubscriptionWithMsgSelTest_129623.class,
               SharedSubscriptionWithMsgSelTest_129626.class, //full 2nd
               SharedSubscriptionTest_129626.class, //full 2nd
               JMSProducer_Test118073.class, //full

               JMSContextTest_118077.class, //full

               DurableUnsharedTest.class,
               JMSContextInjectTest.class, //full

               JMSDefaultConnectionFactoryTest.class,
               JMSEjbJarXmlMdbTest.class,
               JMSDefaultConnFactoryVariationTest.class //full 2nd

})
public class FATSuite {}
