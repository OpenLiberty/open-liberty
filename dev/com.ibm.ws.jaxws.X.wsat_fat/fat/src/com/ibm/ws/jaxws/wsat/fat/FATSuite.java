/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxws.wsat.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
               ServiceIOTest.class,
               WSATAssertionTest.class,

               // For special policy attachment function
               // Defect 199798
               PolicyAttachmentTest.class
})
public class FATSuite {
	
	// TODO: Enable repeats for tests against other buckets (currently failing)
    //@ClassRule
    //public static RepeatTests r = RepeatTests.with(new EmptyAction()).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().removeFeature("jaxws-2.2").addFeature("jaxws-2.3").withID("jaxws-2.3")).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jaxws-2.3"));


}
