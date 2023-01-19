/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.executor.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JAXRSExecutorTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = 
        RepeatTests.withoutModification()
                   .andWith(new JakartaEE10Action())
                   .andWith(new JakartaEE10Action().removeFeature("concurrent-1.0").removeFeature("concurrent-3.0").withID(JakartaEE10Action.ID + "_w/o_concurrent_feature"));
}
