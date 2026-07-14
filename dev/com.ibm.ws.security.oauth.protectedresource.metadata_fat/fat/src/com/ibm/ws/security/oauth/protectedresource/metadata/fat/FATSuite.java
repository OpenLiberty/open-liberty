/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth.protectedresource.metadata.fat;

import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;
import static componenttest.rules.repeater.EERepeatActions.EE12;
import static componenttest.rules.repeater.EERepeatActions.EE7;
import static componenttest.rules.repeater.EERepeatActions.EE8;
import static componenttest.rules.repeater.EERepeatActions.EE9;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;

/**
 * Collection of all FAT tests for OAuth 2.0 Protected Resource Metadata (RFC 8707).
 */
@RunWith(Suite.class)
@SuiteClasses({
    OAuthProtectedResourceMetadataFATTest.class,
    OAuthProtectedResourceMetadataNonBetaFATTest.class
})
public class FATSuite {

    // EE7 runs in LITE mode; EE8–EE12 run in FULL mode.
    // skipTransformation=true: no WARs to transform — the javax/jakarta switch is
    // handled entirely by the feature system (ibm.tolerates on openidConnectClient-1.0).
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null, /* skipTransformation */ true, EE7, EE8, EE9, EE10, EE11, EE12);

}
