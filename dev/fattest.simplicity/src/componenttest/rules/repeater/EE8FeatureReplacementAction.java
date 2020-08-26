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

    static final String[] EE8_FEATURES_ARRAY = { "appClientSupport-1.0",
                                                 "javaee-8.0",
                                                 "webProfile-8.0",
                                                 "javaeeClient-8.0",
                                                 "servlet-4.0",
                                                 "javaMail-1.6",
                                                 "cdi-2.0",
                                                 "jca-1.7",
                                                 "jpa-2.2",
                                                 "beanValidation-2.0",
                                                 "jaxrs-2.1",
                                                 "jaxrsClient-2.1",
                                                 "jsf-2.3",
                                                 "appSecurity-3.0",
                                                 "jsonp-1.1",
                                                 "jsonb-1.0",
                                                 "jsonpContainer-1.1",
                                                 "jsonbContainer-1.0",
                                                 "ejb-3.2",
                                                 "ejbHome-3.2",
                                                 "ejbLite-3.2",
                                                 "ejbPersistentTimer-3.2",
                                                 "ejbRemote-3.2",
                                                 "ejbTest-1.0",
                                                 "el-3.0",
                                                 "jmsMdb-3.2",
                                                 "jsp-2.3",
                                                 "concurrent-1.0",
                                                 "jaxb-2.2",
                                                 "managedBeans-1.0",
                                                 "mdb-3.2",
                                                 "componenttest-1.0",
                                                 "txtest-1.0",
                                                 "websocket-1.1" };

    public static final Set<String> EE8_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE8_FEATURES_ARRAY)));

    public EE8FeatureReplacementAction() {
        super(EE8_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE8 compatibility";
    }

}
