/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.config.fat.repeat;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.utils.tck.TCKUtilities;

/**
 * Contains static methods for creating standard RepeatTests rules for Config tests
 *
 * MicroProfile 1.2 = Config 1.1
 * MicroProfile 1.3 = Config 1.2
 * MicroProfile 1.4 = Config 1.3
 * MicroProfile 2.0 = Config 1.3
 * MicroProfile 2.1 = Config 1.3
 * MicroProfile 2.2 = Config 1.3
 * MicroProfile 3.0 = Config 1.3
 * MicroProfile 3.2 = Config 1.3
 * MicroProfile 3.3 = Config 1.4
 * MicroProfile 4.0 = Config 2.0
 * MicroProfile 4.1 = Config 2.0
 * MicroProfile 5.0 = Config 3.0
 * MicroProfile 6.0 = Config 3.0
 * MicroProfile 6.1 = Config 3.1
 * MicroProfile 7.0 = Config 3.1
 */
public class ConfigRepeatActions {

    /**
     * Return a rule to repeat tests for MicroProfile 7.0, 6.0, 4.1 and 3.3.
     * This translates to Config 3.1, 3.0, 2.0 and 1.4 respectively.
     *
     * Covers the latest version plus most of the other major versions.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return MicroProfileActions.repeatIf(server,
                                            TCKUtilities::areAllFeaturesPresent,
                                            TestMode.FULL,
                                            false,
                                            MicroProfileActions.MP70_EE10, //Config 3.1
                                            MicroProfileActions.MP70_EE11, //Config 3.1
                                            MicroProfileActions.MP60, //Config 3.0
                                            MicroProfileActions.MP41, //Config 2.0
                                            MicroProfileActions.MP33); //Config 1.4
    }

    /**
     * Return a rule to repeat tests for MicroProfile 7.0, 6.0 and 4.1.
     * This translates to Config 3.1, 3.0 and 2.0 respectively.
     *
     * Covers the latest version plus most of the other major versions, up to Config 2.0.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault20Up(String server) {
        return MicroProfileActions.repeatIf(server,
                                            TCKUtilities::areAllFeaturesPresent,
                                            TestMode.FULL,
                                            false,
                                            MicroProfileActions.MP70_EE10, //Config 3.1
                                            MicroProfileActions.MP70_EE11, //Config 3.1
                                            MicroProfileActions.MP60, //Config 3.0
                                            MicroProfileActions.MP41); //Config 2.0
    }

    /**
     * Return a rule to repeat tests for MicroProfile 7.0 and 6.0.
     * This translates to Config 3.1 and 3.0.
     *
     * Covers the latest version plus most of the other major versions, up to Config 3.0.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault30Up(String server) {
        return repeatDefault30Up(server, false);
    }

    /**
     * Return a rule to repeat tests for MicroProfile 7.0 and 6.0.
     * This translates to Config 3.1 and 3.0.
     *
     * Covers the latest version plus most of the other major versions, up to Config 3.0.
     *
     * @param server             the server name
     * @param skipTransformation Skip transformation for actions
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault30Up(String server, boolean skipTransformation) {
        return MicroProfileActions.repeatIf(server,
                                            TCKUtilities::areAllFeaturesPresent,
                                            TestMode.FULL,
                                            skipTransformation,
                                            MicroProfileActions.MP70_EE10, //Config 3.1
                                            MicroProfileActions.MP70_EE11, //Config 3.1
                                            MicroProfileActions.MP60); //Config 3.0
    }

    /**
     * Return a rule to repeat tests for MicroProfile 7.0 and 6.1.
     * This translates to all versions which contain Config 3.1.
     *
     * Covers only Config 3.1. May wish to add newer versions in future.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault31(String server) {
        return MicroProfileActions.repeatIf(server,
                                            TCKUtilities::areAllFeaturesPresent,
                                            TestMode.FULL,
                                            false,
                                            MicroProfileActions.MP70_EE10, //Config 3.1
                                            MicroProfileActions.MP70_EE11, //Config 3.1
                                            MicroProfileActions.MP61); //Config 3.1
    }

    /**
     * Return a rule to repeat tests for all MP versions which contain Config
     * <p>
     * We run a few tests using this rule so that we have some coverage of all implementations and all MP versions.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatAll(String server) {
        return MicroProfileActions.repeatIf(server,
                                            TCKUtilities::areAllFeaturesPresent,
                                            TestMode.FULL,
                                            false,
                                            MicroProfileActions.MP70_EE10,
                                            MicroProfileActions.MP70_EE11,
                                            MicroProfileActions.MP61,
                                            MicroProfileActions.MP60,
                                            MicroProfileActions.MP50,
                                            MicroProfileActions.MP41,
                                            MicroProfileActions.MP40,
                                            MicroProfileActions.MP33,
                                            MicroProfileActions.MP32,
                                            MicroProfileActions.MP30,
                                            MicroProfileActions.MP22,
                                            MicroProfileActions.MP20,
                                            MicroProfileActions.MP13,
                                            MicroProfileActions.MP12);
    }
}
