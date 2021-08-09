/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import componenttest.annotation.FeatureRequiresMinimumJavaLevel;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.topology.impl.JavaInfo;

public class JavaLevelFilter extends Filter {

    public static final String FEATURE_UNDER_TEST;

    static {
        FEATURE_UNDER_TEST = System.getProperty(FeatureFilter.FEATURE_UNDER_TEST_PROPERTY_NAME);
        Log.info(JavaLevelFilter.class, "<clinit>", "System property: " + FeatureFilter.FEATURE_UNDER_TEST_PROPERTY_NAME + " is " + FEATURE_UNDER_TEST);
    }

    private static Class<?> getMyClass() {
        return JavaLevelFilter.class;
    }

    /** {@inheritDoc} */
    @Override
    public String describe() {
        return null;
    }

    /**
     * Like {@link Description#getTestClass}, but without initializing the class.
     */
    private static Class<?> getTestClass(Description desc) {
        try {
            return Class.forName(desc.getClassName(), false, getMyClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldRun(Description desc) {
        MaximumJavaLevel maximumJavaLevelAnnotation = desc.getAnnotation(MaximumJavaLevel.class);
        if (maximumJavaLevelAnnotation == null) {
            //there was no method level annotation
            //check for a test class level annotation
            maximumJavaLevelAnnotation = getTestClass(desc).getAnnotation(MaximumJavaLevel.class);
        }
        if (maximumJavaLevelAnnotation != null) {
            if (JavaInfo.JAVA_VERSION > maximumJavaLevelAnnotation.javaLevel()) {
                Log.debug(getMyClass(), "Removing test " + desc.getMethodName()
                                        + " from list to run, because its maximum java level is " + maximumJavaLevelAnnotation.javaLevel()
                                        + " and we are running with " + JavaInfo.JAVA_VERSION);
                return false;
            }
        }

        MinimumJavaLevel minimumJavaLevelAnnotation = desc.getAnnotation(MinimumJavaLevel.class);
        FeatureRequiresMinimumJavaLevel featureRequiresLevelAnnotation = desc.getAnnotation(FeatureRequiresMinimumJavaLevel.class);
        //check for a method level annotation first

        //method level annotations supercede any class level annotation
        if (minimumJavaLevelAnnotation == null) {
            //there was no method level annotation
            //check for a test class level annotation
            minimumJavaLevelAnnotation = getTestClass(desc).getAnnotation(MinimumJavaLevel.class);
        }
        if (featureRequiresLevelAnnotation == null) {
            //there was no method level annotation
            //check for a test class level annotation
            featureRequiresLevelAnnotation = getTestClass(desc).getAnnotation(FeatureRequiresMinimumJavaLevel.class);
        }

        // If there's a minimum java level annotaton, that sets a global minimum level, so if we don't meet that, don't run tests
        boolean javaLevelTooLowForAllFeatures = minimumJavaLevelAnnotation != null && JavaInfo.JAVA_VERSION < minimumJavaLevelAnnotation.javaLevel();
        if (javaLevelTooLowForAllFeatures) {
            Log.debug(getMyClass(), "Removing test " + desc.getMethodName() + " with minimum java level " + minimumJavaLevelAnnotation.javaLevel()
                                    + " from list to run, because it is too high for current java level "
                                    + JavaInfo.JAVA_VERSION);
            return false;
        } else {
            // Check if this is testing the feature with the minimum level
            boolean applicableFeaturePresent = featureRequiresLevelAnnotation != null && featureRequiresLevelAnnotation.feature().equals(FEATURE_UNDER_TEST);
            if (applicableFeaturePresent) {
                // This feature has a minimum java level, do we meet it?

                if (JavaInfo.JAVA_VERSION < featureRequiresLevelAnnotation.javaLevel()) {
                    Log.debug(getMyClass(), "Removing test " + desc.getMethodName() + " because feature " + featureRequiresLevelAnnotation.feature()
                                            + " from list to run, because it requires java level " + featureRequiresLevelAnnotation.javaLevel()
                                            + " and we are running with "
                                            + JavaInfo.JAVA_VERSION);
                    return false;
                }
            }
        }

        return true;
    }
}
