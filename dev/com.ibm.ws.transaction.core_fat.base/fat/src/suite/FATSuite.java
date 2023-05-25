/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import tests.SimpleTest;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleTest.class,
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .fullFATOnly()
                                    .removeFeature("mpfaulttolerance-1.0")
                                    .addFeature("mpFaultTolerance-4.0"))
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeature("mpfaulttolerance-1.0")
                                    .addFeature("mpFaultTolerance-4.0"));
}