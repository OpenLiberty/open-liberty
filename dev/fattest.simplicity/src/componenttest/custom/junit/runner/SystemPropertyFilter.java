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
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.Util;

/**
 * Filter based on system properties.
 * 
 * This filter uses annotation {@link SkipIfSysProp} to filter test
 * classes and test methods.
 * 
 * The annotation may be placed as a class annotation and as a method
 * annotation.  Placement as a method annotation overrides a class
 * annotation, when one is also present.
 * 
 * The annotation provides a list of values which specify system
 * properties which may be used to disable tests.
 * See {@link #shouldRun(Description)} for more information.
 */
public class SystemPropertyFilter extends Filter {
    /**
     * Answer a print string for this filter.
     * 
     * @return A print string for this filter.
     */
    @Override
    public String describe() {
        return "SystemPropertyFilter";
    }

    /**
     * Tell if a test method is to be run, based on the {@link SkipIfSysProp}
     * annotation that applies to the test method.
     * 
     * A method annotation is used first, followed by a class annotation.
     * 
     * If neither annotation is present, true is returned, indicating that
     * the test should not be skipped.
     * 
     * The annotation provides a list of values which specify system
     * properties which may be used to disable tests.
     * 
     * The list of annotations values has two forms: Either, the annotation
     * value has an embedded equals sign ('='), in which case the annotation
     * specifies a system property name followed by a specific value of
     * that system property which will disable the target of the annotation.
     * Or, the annotation value does not contain an embedded equals sign.
     * Then, the annotation value is a system property name, and if that
     * system property has any assigned value the target of the annotation
     * is skipped.
     * 
     * @param desc The description of the test method which is to be tested.
     * 
     * @return True or false, telling if the test method should be run.
     */
    @Override
    public boolean shouldRun(Description desc) {
        String methodName = "shouldRun";
        
        SkipIfSysProp anno = Util.getAnnotation(SkipIfSysProp.class, desc);
        if ( anno == null ) {
            return true;
        }
        
        for ( String annoValue : anno.value() ) {
            if ( annoValue.contains("=") ) {
                // Only skip if the system property has the specified value.
                String[] propNameAndValue = annoValue.split("=");
                String propName = propNameAndValue[0];
                String targetPropValue = propNameAndValue[1];
                String actualPropValue = System.getProperty(propName);
                if (targetPropValue.equalsIgnoreCase(actualPropValue)) {
                    Log.info(SystemPropertyFilter.class, methodName,
                        "System property " + propName + " has value " + actualPropValue + " but " + targetPropValue + " is required." +
                        " The test " + desc.getDisplayName() + " will be skipped.");
                    return false;
                }
            } else {
                // Skip if any value is specified for the property
                String propName = annoValue;
                String actualPropValue = System.getProperty(annoValue); 
                if ( actualPropValue != null ) {
                    Log.info(SystemPropertyFilter.class, methodName,
                        "System property " + propName + " has value " + actualPropValue + "." +
                        " The test " + desc.getDisplayName() + " will be skipped.");
                    return false;
                }
            }
        }

        // Do not skip: None of the annotation values matches a system property.

        return true;
    }
}
