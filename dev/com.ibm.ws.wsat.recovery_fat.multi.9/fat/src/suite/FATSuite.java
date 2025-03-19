/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import tests.ReroutePeerRecoveryTest;

@RunWith(Suite.class)
@SuiteClasses({
	ReroutePeerRecoveryTest.class,
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
