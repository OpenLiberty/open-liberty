/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
