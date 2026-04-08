/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.tx.jta.fat.hibernate;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                HibernateTxTest.class,
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .addFeature("jdbc-4.2"))
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .addFeature("jdbc-4.2")) // test EE9 with jdbc-4.2
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .removeFeature("jdbc-4.2")
                                    .addFeature("jdbc-4.3")
                                    .withMinJavaLevel(SEVersion.JAVA11)) // test EE9+jdbc4.3. Jdbc4.3 reqs. JSE11
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .removeFeature("jdbc-4.3")
                                    .addFeature("jdbc-4.2")) // test EE10 with jdbc-4.2
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .removeFeature("jdbc-4.2")
                                    .addFeature("jdbc-4.3")) // test EE10+jdbc4.3. Jdbc4.3 reqs. JSE11
                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .removeFeature("jdbc-4.3")
                                    .addFeature("jdbc-4.2")) // test EE11 with jdbc-4.2
                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                    .removeFeature("jdbc-4.1")
                                    .removeFeature("jdbc-4.2")
                                    .addFeature("jdbc-4.3")) // test EE11+jdbc4.3. Jdbc4.3 reqs. JSE11
    ;
}
