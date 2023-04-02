/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.SkipIfSysProp;

public class SystemPropertyFilter extends Filter {

    /** {@inheritDoc} */
    @Override
    public String describe() {
        return null;
    }

    /**
     * Like {@link Description#getTestClass}, but without initializing the class.
     */
    private Class<?> getTestClass(Description desc) {
        try {
            return Class.forName(desc.getClassName(), false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldRun(Description desc) {
        return shouldSkipViaSysProp(desc) ? false : shouldRunViaSysProp(desc);
    }

    /**
     * Decide if we should skip this test based on the SkipIfSysProp annotation
     *
     * @param desc
     * @return true (we must skip), false (we should run unless something else prevents us)
     */
    private boolean shouldSkipViaSysProp(Description desc) {
        SkipIfSysProp anno = desc.getAnnotation(SkipIfSysProp.class);
        //method level annotations supersede any class level annotation
        if (anno == null) {
            //there was no method level annotation
            //check for a test class level annotation
            anno = getTestClass(desc).getAnnotation(SkipIfSysProp.class);
        }

        return shouldSkipViaSysProp(anno, desc.getDisplayName());
    }

    public static boolean shouldSkipViaSysProp(SkipIfSysProp anno, String methodName) {

        //No annotation where found, therefore no reason to skip
        if (anno == null)
            return false;

        for (String sysProp : anno.value()) {
            if (sysProp.contains("=")) {
                // Only skip if the system prop is a certain value
                String[] keyValue = sysProp.split("=");
                String actualPropValue = System.getProperty(keyValue[0]);
                if (keyValue[1].equalsIgnoreCase(actualPropValue)) {
                    Log.info(SystemPropertyFilter.class, "shouldSkipViaSysProp", "System property " + keyValue[0] + " was found with value=" + keyValue[1] +
                                                                                 "  The test " + methodName + " will be skipped.");
                    return true;
                }
            } else {
                // Skip if the system prop is found at all
                if (System.getProperty(sysProp) != null) {
                    Log.info(SystemPropertyFilter.class, "shouldSkipViaSysProp", "System property " + sysProp + " was found.  " +
                                                                                 " The test " + methodName + " will be skipped.");
                    return true;
                }
            }
        }

        // No system properties where found, therefore no reason to skip
        return false;
    }

    /**
     * Decide if we should run this test based on the OnlyIfSysProp annotation
     *
     * @param desc
     * @return true (we should run unless something else prevents us), false (we must skip)
     */
    private boolean shouldRunViaSysProp(Description desc) {
        OnlyIfSysProp anno = desc.getAnnotation(OnlyIfSysProp.class);
        //method level annotations supersede any class level annotation
        if (anno == null) {
            //there was no method level annotation
            //check for a test class level annotation
            anno = getTestClass(desc).getAnnotation(OnlyIfSysProp.class);
        }

        return shouldRunViaSysProp(anno, desc.getDisplayName());
    }

    public static boolean shouldRunViaSysProp(OnlyIfSysProp anno, String methodName) {

        //No annotation found, therefore we should run
        if (anno == null)
            return true;

        for (String sysProp : anno.value()) {
            if (sysProp.contains("=")) {
                // Only run if the system prop is a certain value
                String[] keyValue = sysProp.split("=");
                String actualPropValue = System.getProperty(keyValue[0]);
                if (!keyValue[1].equalsIgnoreCase(actualPropValue)) {
                    Log.info(SystemPropertyFilter.class, "shouldRunViaSysProp", "System property " + keyValue[0] + " was found with value=" + actualPropValue +
                                                                                " but we expected to find value=" + keyValue[1] +
                                                                                "  The test " + methodName + " will be skipped.");
                    return false;
                }
            } else {
                // Only run if the system prop is found at all
                if (System.getProperty(sysProp) == null) {
                    Log.info(SystemPropertyFilter.class, "shouldRunViaSysProp", "System property " + sysProp + " was not found.  " +
                                                                                " The test " + methodName + " will be skipped.");
                    return false;
                }
            }
        }

        // No system properties where found, therefore we should run
        return true;
    }
}
