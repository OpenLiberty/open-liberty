/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.utils.common;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;

/**
 *
 */
public class RepeatWithEE7cbh20 extends FeatureReplacementAction {

    public static final String ID = "EE7cbh-2.0";

    public RepeatWithEE7cbh20() {
        removeFeature("usr:wsseccbh-1.0");
        addFeature("usr:wsseccbh-2.0");
        forceAddFeatures(false);
        withID(ID);
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
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