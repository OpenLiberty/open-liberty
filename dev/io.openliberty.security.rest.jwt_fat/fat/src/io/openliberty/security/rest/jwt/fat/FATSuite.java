/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.security.rest.jwt.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                RestJwtFATTest.class,
                RestJwtNoConnectorFATTest.class,
                RestJwtKeyFileFATTest.class,
                RestJwtKeyFileInvalidFATTest.class
})
public class FATSuite {

    /*
     * The restJwt-1.0 auto feature requires EE10 or later, so only the EE10 repeat is run.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE10RepeatAction().forServerConfigPaths("publish/servers"));

    public static final String REST_MBEANS_ENDPOINT = "/IBMJMXConnectorREST/mbeans";
}
