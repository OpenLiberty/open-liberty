/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction;

import java.util.HashSet;
import java.util.Set;

import componenttest.rules.repeater.FeatureReplacementAction;

/**
 *
 */
public class RepeatWithCDI extends FeatureReplacementAction {

    public static final String ID = "CDIENABLED";

    private static Set<String> featuresToAdd() {
        Set<String> addFeatures = new HashSet<>();
        // We are actually adding appSecurity-3.0 for custom login modules but it pulls in CDI
        addFeatures.add("appSecurity-3.0");
        return addFeatures;
    }

    public RepeatWithCDI() {
        super(featuresToAdd());
        forceAddFeatures(true);
        this.withID(ID);
    }

    public static FeatureReplacementAction WithRepeatWithCDI() {
        return new RepeatWithCDI();
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        return ID;
    }

}
