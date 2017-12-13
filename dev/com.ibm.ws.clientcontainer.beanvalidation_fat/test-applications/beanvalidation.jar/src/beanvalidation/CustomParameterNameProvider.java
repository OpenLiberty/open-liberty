/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beanvalidation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ParameterNameProvider;

/**
 * Simple implementation of a ParameterNameProvider that provides modified
 * parameter names for testing purposes.
 */
public class CustomParameterNameProvider implements ParameterNameProvider {

    @Override
    public List<String> getParameterNames(Constructor<?> arg0) {
        return getParameterNames(arg0.getParameterTypes());
    }

    @Override
    public List<String> getParameterNames(Method arg0) {
        return getParameterNames(arg0.getParameterTypes());
    }

    private List<String> getParameterNames(final Class<?>[] classes) {
        final List<String> list = new ArrayList<String>(classes.length);

        for (int i = 0; i < classes.length; i++) {
            list.add(classes[i].getSimpleName() + "_" + i);
        }

        return list;
    }

}
