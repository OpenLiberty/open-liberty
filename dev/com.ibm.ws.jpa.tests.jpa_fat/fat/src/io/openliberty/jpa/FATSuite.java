/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package io.openliberty.jpa;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.jpa.concurrent_enhancement.TestConcurrentEnhancement;
import io.openliberty.jpa.defaultdatasource.JPADefaultDataSourceTest;
import io.openliberty.jpa.dserror.JPADSErrorTest;
import io.openliberty.jpa.dsoverride.DSOverrideTest;
import io.openliberty.jpa.ejbpassivation.JPAPassivationTest;
import io.openliberty.jpa.emlocking.EMLockingTest;

@RunWith(Suite.class)
@SuiteClasses({
//                JPAFATTest.class,
                TestConcurrentEnhancement.class,
                JPADefaultDataSourceTest.class,
                JPADSErrorTest.class,
                DSOverrideTest.class,
                EMLockingTest.class,
                JPAPassivationTest.class
})
public class FATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(FeatureReplacementAction.EE7_FEATURES())
                    .andWith(FeatureReplacementAction.EE8_FEATURES())
                    .andWith(new RepeatWithJPA20())
                    .andWith(FeatureReplacementAction.EE9_FEATURES())
                    .andWith(FeatureReplacementAction.EE10_FEATURES())
                    .andWith(FeatureReplacementAction.EE11_FEATURES());
}
