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
        SkipIfSysProp anno = desc.getAnnotation(SkipIfSysProp.class);
        //method level annotations supersede any class level annotation
        if (anno == null) {
            //there was no method level annotation
            //check for a test class level annotation
            anno = getTestClass(desc).getAnnotation(SkipIfSysProp.class);
        }

        if (anno == null)
            return true;

        for (String sysProp : anno.value()) {
            if (sysProp.contains("=")) {
                // Only skip if the system prop is a certain value
                String[] keyValue = sysProp.split("=");
                String actualPropValue = System.getProperty(keyValue[0]);
                if (keyValue[1].equalsIgnoreCase(actualPropValue)) {
                    Log.info(SystemPropertyFilter.class, "shouldTestRun", "System property " + keyValue[0] + " was found with value=" + keyValue[1] +
                                                                          "  The test " + desc.getDisplayName() + " will be skipped.");
                    return false;
                }
            } else {
                // Skip if the system prop is found at all
                if (System.getProperty(sysProp) != null) {
                    Log.info(SystemPropertyFilter.class, "shouldTestRun", "System property " + sysProp + " was found.  " +
                                                                          " The test " + desc.getDisplayName() + " will be skipped.");
                    return false;
                }
            }
        }
        // No system properties were found indicating that a test should be skipped
        return true;
    }
}
