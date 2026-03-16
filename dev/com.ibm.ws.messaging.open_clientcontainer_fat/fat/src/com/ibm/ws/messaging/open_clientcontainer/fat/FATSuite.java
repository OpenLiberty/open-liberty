/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.messaging.open_clientcontainer.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
	JMS2AsyncSendTest.class,
	JMS1AsyncSendTest.class,
	MessageListenerTest.class,
	ClientIDTest.class
})
public class FATSuite { 
	
	@ClassRule
    public static RepeatTests repeater = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeature("jaxws-2.2"))
                    .andWith(FeatureReplacementAction.EE10_FEATURES())
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

}
