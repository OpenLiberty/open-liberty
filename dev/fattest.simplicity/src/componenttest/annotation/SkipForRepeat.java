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

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEEAction;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipForRepeat {

    public static final String NO_MODIFICATION = EmptyAction.ID;
    public static final String EE7_FEATURES = EE7FeatureReplacementAction.ID;
    public static final String EE8_FEATURES = EE8FeatureReplacementAction.ID;
    public static final String EE9_FEATURES = JakartaEEAction.EE9_ACTION_ID;
    public static final String EE10_FEATURES = JakartaEEAction.EE10_ACTION_ID;
    public static final String EE11_FEATURES = JakartaEEAction.EE11_ACTION_ID;

    String[] value();

}
