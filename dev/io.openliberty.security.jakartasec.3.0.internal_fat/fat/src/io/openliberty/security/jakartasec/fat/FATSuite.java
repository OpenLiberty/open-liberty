/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.security.jakartasec.fat.tests.InjectionScopedTests;
import io.openliberty.security.jakartasec.fat.tests.SimplestAnnotatedTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SimplestAnnotatedTest.class,
                InjectionScopedTests.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE10RepeatAction().forServerConfigPaths("publish/servers", "publish/shared/config"));

}
