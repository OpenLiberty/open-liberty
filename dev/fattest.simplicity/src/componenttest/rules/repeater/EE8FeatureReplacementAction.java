/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Set;

public class EE8FeatureReplacementAction extends FeatureReplacementAction {

    public static final String ID = "EE8_FEATURES";

    static final String[] EE8_FEATURES_ARRAY = { "javaee-8.0", "webProfile-8.0", "javaeeClient-8.0", "servlet-4.0", "jdbc-4.2", "javaMail-1.6", "cdi-2.0", "jpa-2.2",
                                                 "beanValidation-2.0", "jaxrs-2.1", "jaxrsClient-2.1", "jsf-2.3", "appSecurity-3.0", "jsonp-1.1", "jsonb-1.0", };
    static final Set<String> EE8_FEATURE_SET = new HashSet<>(Arrays.asList(EE8_FEATURES_ARRAY));

    public EE8FeatureReplacementAction() {
        super(EE7FeatureReplacementAction.EE7_FEATURE_SET, EE8_FEATURE_SET);
        withMinJavaLevel(8);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE8 compatibility";
    }

}
