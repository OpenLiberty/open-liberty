/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE11Action;

/**
 *
 */
public class RepeatWithEE11CDI extends JakartaEE11Action {

    public static final String ID = JakartaEE11Action.ID + "_CDIENABLED";

    public RepeatWithEE11CDI() {
        alwaysAddFeature("appSecurity-6.0");
        withID(ID);
    }

    public static FeatureReplacementAction EE11CDI_FEATURES() {
        return new RepeatWithEE11CDI();
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

    @Override
    public RepeatWithEE11CDI withID(String id) {
        return (RepeatWithEE11CDI) super.withID(id);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return super.toString() + " with CDI Enabled (appSecurity-6.0 added)";
    }

}
