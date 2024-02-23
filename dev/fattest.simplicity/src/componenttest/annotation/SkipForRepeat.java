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
package componenttest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEEAction;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipForRepeat {

    String[] value();

    public static final String NO_MODIFICATION = EmptyAction.ID;
    public static final String EE7_FEATURES = EE7FeatureReplacementAction.ID;
    public static final String EE8_FEATURES = EE8FeatureReplacementAction.ID;
    public static final String EE9_FEATURES = JakartaEEAction.EE9_ACTION_ID;
    public static final String EE10_FEATURES = JakartaEEAction.EE10_ACTION_ID;
    public static final String EE11_FEATURES = JakartaEEAction.EE11_ACTION_ID;

    // Cannot use MultivalueSkips.name() since it isn't a constant at compile time so need to actually
    // use the string version of the enum name.
    public static final String EE8_OR_LATER_FEATURES = "EE8_OR_LATER_FEATURES";
    public static final String EE9_OR_LATER_FEATURES = "EE9_OR_LATER_FEATURES";
    public static final String EE10_OR_LATER_FEATURES = "EE10_OR_LATER_FEATURES";
    public static final String EE11_OR_LATER_FEATURES = "EE11_OR_LATER_FEATURES";

    // Using an enum in order to be able to store the array of repeated skip values and to have
    // a static method to process a given list.  Annotations cannot have a method that takes a parameter.
    public enum MultivalueSkips {
        EE8_OR_LATER_FEATURES(new String[] { EE8_FEATURES, EE9_FEATURES, EE10_FEATURES, EE11_FEATURES }),
        EE9_OR_LATER_FEATURES(new String[] { EE9_FEATURES, EE10_FEATURES, EE11_FEATURES }),
        EE10_OR_LATER_FEATURES(new String[] { EE10_FEATURES, EE11_FEATURES }),
        EE11_OR_LATER_FEATURES(new String[] { EE11_FEATURES });

        // These cannot be used as @SkipForRepeat(EE8_OR_LATER_FEATURES.skipValues).  Need to use constants above and then it will
        // be converted to the multiple skip values.
        final String[] skipValues;

        private MultivalueSkips(String[] skipValues) {
            this.skipValues = skipValues;
        }

        private static final Set<String> OR_LATER_NAMES;
        static {
            Set<String> enumNames = new HashSet<>();
            enumNames.add(EE8_OR_LATER_FEATURES.name());
            enumNames.add(EE9_OR_LATER_FEATURES.name());
            enumNames.add(EE10_OR_LATER_FEATURES.name());
            enumNames.add(EE11_OR_LATER_FEATURES.name());
            OR_LATER_NAMES = Collections.unmodifiableSet(enumNames);
        }

        public static String[] getSkipForRepeatValues(String[] skipValues) {
            boolean containsOrLaterSkip = false;
            for (String skipValue : skipValues) {
                if (OR_LATER_NAMES.contains(skipValue)) {
                    containsOrLaterSkip = true;
                    break;
                }
            }

            // If the array of skip values does not include one of the "or later" constants, just return
            // the passed in skipValues array.
            if (!containsOrLaterSkip) {
                return skipValues;
            }

            // Otherwise process the "or later" constant(s) and include the values in a final
            // skip value array.
            Set<String> updatedSkipValues = new LinkedHashSet<>();
            for (String skipValue : skipValues) {
                if (!OR_LATER_NAMES.contains(skipValue)) {
                    updatedSkipValues.add(skipValue);
                } else {
                    MultivalueSkips skipEnum = valueOf(skipValue);
                    for (String multiSkipValue : skipEnum.skipValues) {
                        updatedSkipValues.add(multiSkipValue);
                    }
                }
            }
            return updatedSkipValues.toArray(new String[updatedSkipValues.size()]);
        }
    }
}
