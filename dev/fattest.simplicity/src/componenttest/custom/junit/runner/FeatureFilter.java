/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package componenttest.custom.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

/**
 *
 */
public class FeatureFilter extends Filter {

    public static final String FEATURE_UNDER_TEST_PROPERTY_NAME = "fat.test.feature.under.test";

    public static final String FEATURE_UNDER_TEST;

    static {
        FEATURE_UNDER_TEST = System.getProperty(FEATURE_UNDER_TEST_PROPERTY_NAME);
        Log.info(FeatureFilter.class, "<clinit>", "System property: " + FEATURE_UNDER_TEST_PROPERTY_NAME + " is " + FEATURE_UNDER_TEST);
    }

    /** {@inheritDoc} */
    @Override
    public String describe() {
        return "only run when feature is " + FEATURE_UNDER_TEST;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldRun(Description desc) {
        RunIfFeatureBeingTested requiredFeature = desc.getAnnotation(RunIfFeatureBeingTested.class);
        RunUnlessFeatureBeingTested excludedFeature = desc.getAnnotation(RunUnlessFeatureBeingTested.class);;
        //check for a method level annotation first

        //method level annotations supercede any class level annotation
        if (requiredFeature == null) {
            //there was no method level annotation
            //check for a test class level annotation
            requiredFeature = FilterUtils.getTestClass(desc, getClass()).getAnnotation(RunIfFeatureBeingTested.class);
        }
        if (excludedFeature == null) {
            //there was no method level annotation
            //check for a test class level annotation
            excludedFeature = FilterUtils.getTestClass(desc, getClass()).getAnnotation(RunUnlessFeatureBeingTested.class);
        }

        boolean requiredFeatureNotPresent = requiredFeature != null && !requiredFeature.value().equals(FEATURE_UNDER_TEST);
        boolean excludedFeaturePresent = excludedFeature != null && excludedFeature.value().equals(FEATURE_UNDER_TEST);

        if (requiredFeatureNotPresent) {
            Log.debug(getClass(), "Removing test " + desc.getMethodName() + " with required feature " + requiredFeature
                                  + " from list to run, because not valid for current feature under test "
                                  + FEATURE_UNDER_TEST);
            return false;
        } else if (excludedFeaturePresent) {
            Log.debug(getClass(), "Removing test " + desc.getMethodName() + " with \"run-unless\" feature " + excludedFeature
                                  + " from list to run, because it matches current feature under test "
                                  + FEATURE_UNDER_TEST);
            return false;
        } else
            return true;
    }

}
