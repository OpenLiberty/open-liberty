/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.graphql.fat.repeat;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.FeatureSet;

/**
 * Contains static methods for creating standard RepeatTests rules for Graph QL tests
 *
 * MicroProfile 3.3 = Graph QL 1.0
 * MicroProfile 4.0 = Graph QL 1.0
 * MicroProfile 4.1 = Graph QL 1.0
 * MicroProfile 5.0 = Graph QL 2.0
 * MicroProfile 6.0 = Graph QL 2.0
 * MicroProfile 6.1 = Graph QL 2.0
 * MicroProfile 7.0 = Graph QL 2.0
 */
public class GraphQlRepeatActions {

    /**
     * Return a rule to repeat tests for MicroProfile 7.0, 6.1, 5.0, 4.0 and 3.3.
     * This translates to GraphQL 2.0 and 1.0 respectively
     *
     * Covers the latest version plus most of the other major versions.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return MicroProfileActions.repeat(server, TestMode.FULL,
                                          MicroProfileActions.MP70_EE10, //Graph QL 2.0
                                          MicroProfileActions.MP70_EE11, //Graph QL 2.0
                                          MicroProfileActions.MP61, //Graph QL 2.0
                                          MicroProfileActions.MP50, //Graph QL 2.0
                                          MicroProfileActions.MP40, //Graph QL 1.0
                                          MicroProfileActions.MP33); //Graph QL 1.0
    }
}
