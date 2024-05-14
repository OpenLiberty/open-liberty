/*******************************************************************************
 * Copyright (c) 2017,2024 IBM Corporation and others.
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
package com.ibm.ws.bval.v20.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                BeanVal20Test.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //run all tests as-is
                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly()) //run all tests again with EE9 features+packages (full fat only as of EE10)
                    .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly()) //run all tests again with EE10 features+packages (full fat only as of EE11)
                    .andWith(FeatureReplacementAction.EE11_FEATURES()); //run all tests again with EE11 features+packages

}
