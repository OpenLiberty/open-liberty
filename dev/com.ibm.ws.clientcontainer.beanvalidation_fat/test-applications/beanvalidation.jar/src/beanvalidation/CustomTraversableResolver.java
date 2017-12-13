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

import java.lang.annotation.ElementType;

import javax.validation.Path;
import javax.validation.Path.Node;
import javax.validation.TraversableResolver;

/**
 * Simple implementation of a TraversableResolver that tolerates a null
 * parameter for testing purposes.
 */
public class CustomTraversableResolver implements TraversableResolver {

    @Override
    public boolean isCascadable(Object arg0, Node arg1, Class<?> arg2, Path arg3, ElementType arg4) {
        if (arg0.toString().equals("non-cascadable") &&
            arg1 == null &&
            arg2 == null &&
            arg3 == null &&
            arg4 == null) {

            return false;
        }
        return true;
    }

    @Override
    public boolean isReachable(Object arg0, Node arg1, Class<?> arg2, Path arg3, ElementType arg4) {
        if (arg0.toString().equals("non-reachable") &&
            arg1 == null &&
            arg2 == null &&
            arg3 == null &&
            arg4 == null) {

            return false;
        }
        return true;
    }

}
