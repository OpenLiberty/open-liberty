/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

public class EEFeaturesSets {

    private static final String[] EE6_FEATURES_ARRAY = { "javaee-6.0", "beanValidation-1.0", "webProfile-6.0", "ejbLite-3.1", "javaeeClient-6.0", "servlet-3.0", "jdbc-4.0", "cdi-1.0", "jpa-2.0",
                                                 "jaxrs-1.1", "jaxrsClient-1.1", "jsf-2.0", "appSecurity-2.0", "jsp-2.2", "componenttest-1.0", "concurrent-1.0"};
    private static final String[] EE7_FEATURES_ARRAY = { "javaee-7.0", "webProfile-7.0", "javaeeClient-7.0", "servlet-3.1", "jdbc-4.1", "javaMail-1.5", "cdi-1.2", "jpa-2.1",
                                                 "beanValidation-1.1", "jaxrs-2.0", "jaxrsClient-2.0", "jsf-2.2", "appSecurity-2.0", "jsonp-1.0", "ejbLite-3.2", "jsp-2.3", "componenttest-1.0", "concurrent-1.0"};
    private static final String[] EE8_FEATURES_ARRAY = { "javaee-8.0", "webProfile-8.0", "javaeeClient-8.0", "servlet-4.0", "jdbc-4.2", "javaMail-1.6", "cdi-2.0", "jpa-2.2",
                                                 "beanValidation-2.0", "jaxrs-2.1", "jsf-2.3", "appSecurity-3.0", "jsonp-1.1", "jsonb-1.0", "jsp-2.3", "ejbLite-3.2", "componenttest-1.0", "concurrent-1.0" };


    // Point-in-time list of enabled JakartaEE9 features.
    // This list is of only the currently enabled features.
    //
    // FAT tests use a mix of enabled features and not yet enabled
    // features, which is necessary for the FATs to run.

    private static final String[] EE9_FEATURES_ARRAY = { "jakartaee-9.0", "webProfile-9.0", "jakartaeeClient-9.0", "componenttest-2.0", // replaces "componenttest-1.0", 
                                                 "beanValidation-3.0", "cdi-3.0", "concurrent-2.0", "el-4.0", "javaMail-2.0", "jaxrs-3.0", "jaxrsClient-3.0",
                                                 "jpa-3.0", "jsonp-2.0", "jsonb-2.0", "jsonpContainer-2.0", "jsonbContainer-2.0", "jsf-3.0", "jsp-3.0", "servlet-5.0"};

    public static final Set<String> EE6_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE6_FEATURES_ARRAY)));
    public static final Set<String> EE7_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE7_FEATURES_ARRAY)));
    public static final Set<String> EE8_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE8_FEATURES_ARRAY)));
    public static final Set<String> EE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));

    public static final Set<String> ALL_EE_FEATURE_SET;

    static {
        Set<String> allSets = new HashSet<String>();
        allSets.addAll(EE6_FEATURE_SET);
        allSets.addAll(EE7_FEATURE_SET);
        allSets.addAll(EE8_FEATURE_SET);
        ALL_EE_FEATURE_SET = Collections.unmodifiableSet(allSets);
    }

}

