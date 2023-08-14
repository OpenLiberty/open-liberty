/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.transport.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Utilities to assist in copying objects
 */
public class CopyUtils {

    /**
     * Make a shallow copy of a collection
     *
     * @param <T>        the element tType of the collection
     * @param collection the collection
     * @return the copied collection, {@code null} if {@code collection} is {@code null}
     */
    public static <T> List<T> copyCollection(Collection<T> collection) {
        if (collection == null) {
            return null;
        } else {
            return new ArrayList<>(collection);
        }
    }

    /**
     * Make a deep copy of a collection
     *
     * @param <T>        the element type of the collection
     * @param collection the collection to copy
     * @param clone      a function to clone an element
     * @return the copied collection, {@code null} if {@code collection} is {@code null}
     */
    public static <T> List<T> copyCollection(Collection<T> collection, UnaryOperator<T> clone) {
        if (collection == null) {
            return null;
        }

        ArrayList<T> result = new ArrayList<T>();
        for (T element : collection) {
            result.add(clone.apply(element));
        }
        return result;
    }

    /**
     * Copy a {@link Date}
     *
     * @param date the date to copy
     * @return the copy, {@code null} if {@code date} is @{code null}
     */
    public static Date copyDate(Date date) {
        if (date == null) {
            return null;
        }

        return (Date) date.clone();
    }

    /**
     * Copy a {@link Calendar}
     *
     * @param calendar the calendar to copy
     * @return the copy, @{code null} if {@code calendar} is @{code null}
     */
    public static Calendar copyCalendar(Calendar calendar) {
        if (calendar == null) {
            return null;
        }

        return (Calendar) calendar.clone();
    }

    /**
     * Copy an object with a copy constructor, allowing @{code null}
     * <p>
     *
     * @param <T>             the object type
     * @param object          the object to copy
     * @param copyConstructor the copy constructor, usually a method reference
     * @return the copy, or @{code null} if {@code object} was @{code null}
     */
    public static <T> T copyObject(T object, UnaryOperator<T> copyConstructor) {
        if (object == null) {
            return null;
        }
        return copyConstructor.apply(object);
    }

}
