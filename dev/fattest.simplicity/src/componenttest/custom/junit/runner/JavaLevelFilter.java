/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import componenttest.annotation.Util;
import componenttest.topology.impl.JavaInfo;

/**
 * Java level based test filter.
 * 
 * This filter uses the annotations {@link MaximumJavaLevel},
 * {@link MinimumJavaLevel}, and {@link FeatureRequiresMinimumJavaLevel}
 * to filter test classes and test methods.  See {@link #shouldRun(Description)}
 * for more information.
 */
public class JavaLevelFilter extends Filter {
    private static final Class<? extends JavaLevelFilter> c = JavaLevelFilter.class;

    /**
     * Answer a print string for this filter.
     * 
     * @return A print string for this filter.
     */    
    @Override
    public String describe() {
        return "JavaLevelFilter(" + JavaInfo.JAVA_VERSION + ")";
    }

    /**
     * Tell if this test should be run.
     *
     * Test filter annotation values against the java level obtained
     * from {@link JavaInfo#JAVA_VERSION}.
     * 
     * Filter values are specified using {@link MinimimJavaLevel},
     * {@link MaximumJavaLevel}, and {@link FeatureRequiresMinimumJavaLevel}.
     * 
     * If a minimum java level is specified, that level must be less than or
     * equal to the java level.  If a maximum java level is specified, that level
     * must be less than or equal to the java level.
     * 
     * If a feature is set, the feature java level must be less than or equal to
     * the java level.
     *
     * @param desc The description of the test method which is to be tested.
     *
     * @return True or false telling if the test method is to be run.
     */
    @Override
    public boolean shouldRun(Description desc) {
        String methodName = "shouldRun";

        MaximumJavaLevel maxJavaLevel = Util.getAnnotation(MaximumJavaLevel.class, desc);
        if ( maxJavaLevel != null ) {
            if (JavaInfo.JAVA_VERSION > maxJavaLevel.javaLevel()) {
                Log.info(c, methodName,
                    "Skipping test " + desc.getMethodName() +
                    "; the test maximum java level is " + maxJavaLevel.javaLevel() +
                    "; the current java level is " + JavaInfo.JAVA_VERSION);
                return false;
            }
        }

        MinimumJavaLevel minJavaLevel = Util.getAnnotation(MinimumJavaLevel.class, desc);
        if ( (minJavaLevel != null) && JavaInfo.JAVA_VERSION < minJavaLevel.javaLevel() ) {
            Log.info(c, methodName,
                "Skipping test " + desc.getMethodName() +
                "; the test minimum java level is " + minJavaLevel.javaLevel() +
                "; the current java level is " + JavaInfo.JAVA_VERSION);
            return false;
        }

        if ( FeatureFilter.FEATURE_UNDER_TEST != null ) {
            FeatureRequiresMinimumJavaLevel featureMinJavaLevel =
                Util.getAnnotation(FeatureRequiresMinimumJavaLevel.class, desc);
            if ( (featureMinJavaLevel != null) && featureMinJavaLevel.feature().equals(FeatureFilter.FEATURE_UNDER_TEST) ) {
                if ( JavaInfo.JAVA_VERSION < featureMinJavaLevel.javaLevel() ) {
                    Log.info(c,  methodName,
                        "Skipping test " + desc.getMethodName() +
                        " which uses feature " + featureMinJavaLevel.feature() +
                        "; the feature minimum java level is " + featureMinJavaLevel.javaLevel() +
                        "; the current java level is " + JavaInfo.JAVA_VERSION);
                    return false;
                }
            }
        }

        return true;
    }
}
