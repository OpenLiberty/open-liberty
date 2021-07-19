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
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({

DummyTest.class, DeliveryDelaySecOffFullModeTest.class,
		DeliveryDelaySecOffLiteModeTest.class, DeliveryDelayFullModeTest.class,
		DeliveryDelaySecOnFullModeTest.class,
		DeliveryDelaySecOnLiteModeTest.class

})
public class FATSuite {
}
