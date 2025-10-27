/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat.actions;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;

/**
 * This action ensures that mpConfig-3.0 is replaced with mpConfig-1.3 when running with EE7 features.
 * This is necessary because mpConfig-3.0 requires Jakarta EE 9+ features, which are incompatible with EE7.
 */
public class MPConfigCompatibilityAction extends FeatureReplacementAction {

    public static final String ID = "MP_CONFIG_COMPATIBILITY";

    public MPConfigCompatibilityAction() {
        super("mpConfig-3.0", "mpConfig-1.3");
        withID(ID);
        forServers("DefaultAllUpServer");
    }

    /**
     * This action is only enabled when running with EE7 features and mpHealth-4.0
     */
    @Override
    public boolean isEnabled() {
        // Check if we're running with EE7 features and mpHealth-4.0
        String testMode = System.getProperty("fat.test.mode");
        return testMode != null && testMode.contains(EE7FeatureReplacementAction.ID) &&
               testMode.contains("MPHEALTH40");
    }
}
