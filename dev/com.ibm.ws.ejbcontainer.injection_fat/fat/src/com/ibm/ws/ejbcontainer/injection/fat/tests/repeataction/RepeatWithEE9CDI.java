/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;

/**
 *
 */
public class RepeatWithEE9CDI extends JakartaEE9Action {

    public static final String ID = JakartaEE9Action.ID + "_CDIENABLED";

    public RepeatWithEE9CDI() {
        alwaysAddFeature("appSecurity-4.0");
        withID(ID);
    }

    public static FeatureReplacementAction EE9CDI_FEATURES() {
        return new RepeatWithEE9CDI();
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

    @Override
    public RepeatWithEE9CDI withID(String id) {
        return (RepeatWithEE9CDI) super.withID(id);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return super.toString() + " with CDI Enabled (appSecurity-4.0 added)";
    }

}
