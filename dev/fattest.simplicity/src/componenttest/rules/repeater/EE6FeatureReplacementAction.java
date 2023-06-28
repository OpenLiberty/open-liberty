/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
package componenttest.rules.repeater;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EE6FeatureReplacementAction extends FeatureReplacementAction {

    public static final String ID = "EE6_FEATURES";

    static final String[] EE6_FEATURES_ARRAY = { "javaee-6.0",
                                                 "webProfile-6.0",
                                                 "javaeeClient-6.0",
                                                 "cdi-1.0",
                                                 "appSecurity-2.0",
                                                 "jsf-2.0",
                                                 "jpa-2.0",
                                                 "beanValidation-1.0",
                                                 "jsp-2.2",
                                                 "servlet-3.0",
                                                 "ejbLite-3.1",
                                                 "ejbTest-1.0",
                                                 "jmsMdb-3.1",
                                                 "managedBeans-1.0",
                                                 "mdb-3.1",
                                                 "jaxrs-1.1",
                                                 "jca-1.6",
                                                 "componenttest-1.0" };

    public static final Set<String> EE6_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE6_FEATURES_ARRAY)));

    public EE6FeatureReplacementAction() {
        super(EE6_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        removeFeatures(JakartaEE10Action.EE10_FEATURE_SET);
        removeFeatures(JakartaEE11Action.EE11_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE6 compatibility";
    }

}
