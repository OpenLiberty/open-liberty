/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.annotation;

import java.lang.annotation.Annotation;

import org.junit.runner.Description;

/**
 * Annotations utility methods.
 */
public class Util {
    /**
     * Retrieve an annotation instance from a description.
     *
     * If the method annotation is not available, retrieve
     * the class annotation.
     * 
     * If the class cannot be loaded, answer null.
     *
     * The class of the test method may be loaded, but is not
     * initialized.
     *
     * @param <T> The type of the annotation which is to be retrieved.
     * @param annotationClass The class of the annotation which is 
     *     to be retrieved.
     * @param desc The description of the test method.
     *
     * @return The annotation of the specified value from the
     *     test method.
     */
    public static <T extends Annotation>
        T getAnnotation(Class<T> annotationClass, Description desc) {

        T methodValue = desc.getAnnotation(annotationClass);
        if ( methodValue != null ) {
            return methodValue;
        }
        
        Class<?> testClass = getTestClass(desc);
        if ( testClass == null ) {
            return null;
        }
        
        T classValue = testClass.getAnnotation(annotationClass);
        return classValue;
    }
    
    /**
     * Load but do not initialize the class of a test method.
     * 
     * Answer null if the class cannot be loaded.  Ignore the
     * exception which occurs.
     *
     * @param desc The description of a test method.
     *
     * @return The class of the test method.
     */
    public static Class<?> getTestClass(Description desc) {
        try {
            return Class.forName(desc.getClassName(), false, Util.class.getClassLoader());
        } catch ( ClassNotFoundException e ) {
            return null;
        }
    }    
}
