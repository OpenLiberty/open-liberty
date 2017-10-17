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
package com.ibm.ws.cdi12.test.utils;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.spi.AnnotatedType;

public final class Utils {

    /**
     * A way to identify classes using a String. Extracted out for brevity and ability to easily refactor implementation if necessary.
     */
    public static String id(final Class<?> c) {
        return c.getSimpleName();
    }

    public static String id(final AnnotatedType<?> type) {
        return id(type.getJavaClass());
    }

    public static <E> List<E> reverse(final List<E> list) {
        Collections.reverse(list);
        return list;
    }

    private Utils() {}

}
