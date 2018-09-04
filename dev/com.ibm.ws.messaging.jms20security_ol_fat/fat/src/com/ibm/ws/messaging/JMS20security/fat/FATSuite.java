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
package com.ibm.ws.messaging.JMS20security.fat;

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

@RunWith(Suite.class)
@SuiteClasses({

DummyTest.class, JMSContextTest.class, JMSConsumerTest.class,
		JMSContextTest_118066.class, JMSContextTest_118068.class,
		JMSContextTest_118065.class, DurableUnshared.class,
		JMSContextInjectTest.class,
		JMSDefaultConnectionFactorySecurityTest.class, JMSMDBTest.class

})
public class FATSuite {
}
