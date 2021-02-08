/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.utils;

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
