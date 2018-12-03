package com.ibm.ws.messaging.comms.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;




@RunWith(Suite.class)
@SuiteClasses({ 
	            
				FeatureUpdate.class,
				//WasJmsOutBoundTest.class
	})

public class FATSuite {}
