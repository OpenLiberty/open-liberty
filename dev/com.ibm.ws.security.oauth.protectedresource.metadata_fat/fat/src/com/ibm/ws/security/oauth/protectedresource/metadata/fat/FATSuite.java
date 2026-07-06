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
import static componenttest.rules.repeater.EERepeatActions.EE9;

import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.RepeatActions;
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

    // All known EE feature sets, newest first (required by RepeatActions).
    private static final List<FeatureSet> ALL_EE = Arrays.asList(
            EERepeatActions.EE12, EERepeatActions.EE11, EERepeatActions.EE10, EERepeatActions.EE9,
            EERepeatActions.EE8, EERepeatActions.EE7, EERepeatActions.EE6);

    // withoutModification: runs server.xml as-is (servlet-3.1/javax) in LITE mode — covers the ee-6.0 feature.
    // EE9 (first Jakarta tier) also runs in LITE mode.
    // EE10, EE11, and EE12 run in FULL mode.
    // skipTransformation=true because the Jakarta servlet variant is provided by a separate bundle.
    @ClassRule
    public static RepeatTests r = RepeatTests
            .withoutModification()
            .andWith(RepeatActions.forFeatureSet(ALL_EE, EE9, new String[0], TestMode.LITE, /* skipTransformation */ true))
            .andWith(RepeatActions.forFeatureSet(ALL_EE, EE10, new String[0], TestMode.FULL, /* skipTransformation */ true))
            .andWith(RepeatActions.forFeatureSet(ALL_EE, EE11, new String[0], TestMode.FULL, /* skipTransformation */ true))
            .andWith(RepeatActions.forFeatureSet(ALL_EE, EE12, new String[0], TestMode.FULL, /* skipTransformation */ true));

}
