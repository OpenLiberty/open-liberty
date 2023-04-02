/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import componenttest.rules.repeater.JakartaEE10Action;

/**
 *
 */
public class RepeatWithEE10CDI extends JakartaEE10Action {

    public static final String ID = JakartaEE10Action.ID + "_CDIENABLED";

    public RepeatWithEE10CDI() {
        alwaysAddFeature("appSecurity-5.0");
        withID(ID);
    }

    public static FeatureReplacementAction EE10CDI_FEATURES() {
        return new RepeatWithEE10CDI();
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

    @Override
    public RepeatWithEE10CDI withID(String id) {
        return (RepeatWithEE10CDI) super.withID(id);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return super.toString() + " with CDI Enabled (appSecurity-5.0 added)";
    }

}
