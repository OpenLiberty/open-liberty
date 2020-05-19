/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EE8FeatureReplacementAction extends FeatureReplacementAction {

    public static final String ID = "EE8_FEATURES";

    public EE8FeatureReplacementAction() {
        super(EEFeaturesSets.ALL_EE_FEATURE_SET, EEFeaturesSets.EE8_FEATURE_SET);
        withMinJavaLevel(8);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE8 compatibility";
    }

}
