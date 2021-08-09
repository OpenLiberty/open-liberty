/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.common.enums;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * A helper class to express filter expressions for resources.
 */
public class FilterPredicate {

    private FilterableAttribute attribute;

    public FilterableAttribute getAttribute() {
        return attribute;
    }

    private Collection<String> values;

    /**
     * A predicate may have one or more than one associated value. For example, an 'equals' predicate will
     * just have one, but an 'is one of' predicate might have many. N.B. Only the equals predicate has been
     * implemented so far.
     *
     * @return
     */
    public Collection<String> getValues() {
        return values;
    }

    private FilterPredicate() {

    }

    /**
     * Create a predicate specifying that the value of the specified attribute must exactly
     * match the supplied value. The value must also be the type specified by the FilterableAttribute
     *
     * @param attribute The attribute to match on
     * @param value The exact value that the attribute must have
     * @throws IllegalArgumentException if the value is not of the type returned by the attributes getType method
     */
    public static FilterPredicate areEqual(FilterableAttribute attribute, Object value) {
        FilterPredicate pred = new FilterPredicate();
        pred.attribute = attribute;
        Class<?> requiredType = attribute.getType();
        if (!requiredType.isInstance(value)) {
            throw new IllegalArgumentException("The value must be of the correct type for the FilterableAttribute."
                                               + " Expected: " + requiredType.getName() + " but was " + value.getClass().getName());
        }

        pred.values = Collections.singleton(getString(value));
        return pred;
    }

    /**
     * From the 'value' object, find the field name that will be filtered on, which is a effectively a key into
     * a JSON object. This will generally be the result of calling toString on the object, but
     * for enums, there maybe a getValue method which be used instead.
     *
     * @param value
     * @return
     */
    private static String getString(Object value) {

        // The logic here is slightly icky, as getValue may or may not exist,
        // and must be called reflectively if it does exist

        if (!(value instanceof Enum)) {
            return value.toString();
        }

        Method method = null;
        try {
            method = value.getClass().getMethod("getValue");
        } catch (NoSuchMethodException e) {
        } catch (SecurityException e) {
        }
        if (method != null && method.getReturnType() != String.class) {
            method = null;
        }

        if (method == null) {
            // There was no appropriate getValue method, so rely on toString
            return value.toString();
        }

        // An error at this point is unrecoverable, something is really wrong
        String answer = null;
        try {
            answer = (String) method.invoke(value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("The enum " + value.getClass().getName()
                                       + " was expected to have a public getValue method", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("The enum " + value.getClass().getName()
                                       + " was expected to have a public getValue method with no arguments", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("The enum " + value.getClass().getName()
                                       + " getValue method threw an unexpectd exception", e);
        }

        return answer;

    }

}
