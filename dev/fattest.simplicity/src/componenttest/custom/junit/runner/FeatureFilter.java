/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Util;

/**
 * Feature based test filter.
 * 
 * Optionally, a feature under test may be specified by system property
 * {@link #FEATURE_UNDER_TEST_PROPERTY_NAME}, filter tests using the
 * annotations:
 *
 * <ul>
 * <li>{@link componenttest.annotation.RunIfFeatureBeingTested}</li>
 * <li>{@link RunIfFeatureBeingTested}</li>
 * <li>{@link componenttest.annotation.RunUnlessFeatureBeingTested}</li>
 * <li>{@link RunUnlessFeatureBeingTested}</li>
 * </ul>
 * 
 * These annotations may be provided as method annotations and as class
 * annotations.
 */
public class FeatureFilter extends Filter {
    private static final Class<? extends FeatureFilter> c = FeatureFilter.class;

    /** The name of the property used to set the test feature. */
    public static final String FEATURE_UNDER_TEST_PROPERTY_NAME =
        "fat.test.feature.under.test";

    /** The active test feature.  Often null. */
    public static final String FEATURE_UNDER_TEST;
    static {
        FEATURE_UNDER_TEST = System.getProperty(FEATURE_UNDER_TEST_PROPERTY_NAME);
        Log.info(c, "<clinit>",
            "Test feature (" + FEATURE_UNDER_TEST_PROPERTY_NAME + "): " + FEATURE_UNDER_TEST);
    }

    /**
     * Describe this feature filter.
     * 
     * The feature filter is inactive unless {@link #FEATURE_UNDER_TEST}
     * was set, using system property {@link #FEATURE_UNDER_TEST_PROPERTY_NAME}.
     * 
     * @return A print string for this filter.
     */
    @Override
    public String describe() {
        return "FeatureFilter(" + FEATURE_UNDER_TEST + ")";
    }

    /**
     * Tell if a test method should be run.
     * 
     * Feature filtering is inactive unless system property
     * {@link #FEATURE_UNDER_TEST_PROPERTY_NAME} is set.
     * 
     * When a test feature is set, the presence of a "run-if"
     * annotation means the test is skipped unless the "run-if"
     * annotation matches the test feature.
     * 
     * When a test feature is set, the presence of a "run-unless"
     * annotation means the test is skipped if the "run-unless"
     * annotation matches the test feature.
     * 
     * @param desc The description of the method which is to be tested.
     * 
     * @return True or false telling if the test method is to be run.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldRun(Description desc) {
        String methodName = "shouldRun";

        // Feature filtering is not active.
        if ( FEATURE_UNDER_TEST == null ) {
            return true;
        }

        componenttest.annotation.RunIfFeatureBeingTested requiredFeatureNew =
            Util.getAnnotation(componenttest.annotation.RunIfFeatureBeingTested.class, desc);
        RunIfFeatureBeingTested requiredFeatureDep =
            Util.getAnnotation(RunIfFeatureBeingTested.class, desc);
        
        componenttest.annotation.RunUnlessFeatureBeingTested excludedFeatureNew =
            Util.getAnnotation(componenttest.annotation.RunUnlessFeatureBeingTested.class, desc);
        RunUnlessFeatureBeingTested excludedFeatureDep =
            Util.getAnnotation(RunUnlessFeatureBeingTested.class, desc);

        String requiredFeature;
        if ( (requiredFeatureNew != null) && !requiredFeatureNew.value().equals(FEATURE_UNDER_TEST) ) {
            requiredFeature = requiredFeatureNew.value();
        } else if ( (requiredFeatureDep != null) && !requiredFeatureDep.value().equals(FEATURE_UNDER_TEST) ) {
            requiredFeature = requiredFeatureDep.value();
        } else {
            requiredFeature= null;
        }

        String excludedFeature;
        if ( (excludedFeatureNew != null) && excludedFeatureNew.value().equals(FEATURE_UNDER_TEST) ) {
            excludedFeature = excludedFeatureNew.value();
        } else if ( (excludedFeatureDep != null) && excludedFeatureDep.value().equals(FEATURE_UNDER_TEST) ) {
            excludedFeature = excludedFeatureDep.value();
        } else {
            excludedFeature = null;
        }

        if ( requiredFeature != null ) {
            Log.info(c, methodName,
                "Skipping test " + desc.getMethodName() +
                "; which requires feature " + requiredFeature +
                "; tests are running with feature " + FEATURE_UNDER_TEST);
            return false;

        } else if ( excludedFeature != null ) {
            Log.info(c, methodName,
                "Skipping test " + desc.getMethodName() +
                "; which forbids current running feature " + excludedFeature);
            return false;

        } else {
            return true;
        }
    }
}
