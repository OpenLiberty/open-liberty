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
package io.openliberty.microprofile.openapi20.merge;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.annotation.Trivial;

import java.util.Objects;
import java.util.Optional;

/**
 * Utilities for recursively comparing OpenAPI model objects
 */
public class ModelEquality {

    /**
     * Recursively compare two objects for equality.
     * <ul>
     * <li>If {@code a} and {@code b} are model objects, recursively compare each of their properties</li>
     * <li>If {@code a} and {@code b} are lists, recursively compare each item</li>
     * <li>If {@code a} and {@code b} are maps, ensure the key set is the same and the recursively compare the values</li>
     * <li>Otherwise, use {@link Objects#equals(Object, Object)} to compare {@code a} and {@code b}
     * </ul>
     * 
     * @param a the first item to compare
     * @param b the second item to compare
     * @return {@code true} if {@code a} and {@code b} are equal, otherwise {@code false}
     */
    public static boolean equals(Object a, Object b) {
        return equalsImpl(a, b);
    }
    
    @Trivial
    private static boolean equalsImpl(Object a, Object b) {
        if (a == b) {
            return true;
        }

        if (a == null) {
            if (b == null) {
                return true;
            } else {
                return false;
            }
        } else if (b == null) {
            return false;
        }

        Optional<ModelType> modelObject = ModelType.getModelObject(a.getClass());
        if (modelObject.isPresent()) {
            return equalsModelObject(modelObject.get(), a, b);
        } else if (a instanceof List) {
            if (!(b instanceof List)) {
                return false;
            }
            return equalsList((List<?>) a, (List<?>) b);
        } else if (a instanceof Map) {
            if (!(b instanceof Map)) {
                return false;
            }
            return equalsMap((Map<?, ?>) a, (Map<?, ?>) b);
        } else {
            return Objects.equals(a, b);
        }
    }

    @Trivial
    private static boolean equalsMap(Map<?, ?> a, Map<?, ?> b) {
        if (!Objects.equals(a.keySet(), b.keySet())) {
            return false;
        }

        for (Entry<?, ?> entry : a.entrySet()) {
            if (!equalsImpl(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }

    @Trivial
    private static boolean equalsList(List<?> a, List<?> b) {
        if (a.size() != b.size()) {
            return false;
        }

        Iterator<?> ai = a.iterator();
        Iterator<?> bi = b.iterator();
        while (ai.hasNext()) {
            if (!equalsImpl(ai.next(), bi.next())) {
                return false;
            }
        }

        return true;
    }

    @Trivial
    private static boolean equalsModelObject(ModelType modelType, Object a, Object b) {
        if (!modelType.isInstance(b)) {
            return false;
        }

        for (ModelType.ModelParameter p : modelType.getParameters()) {
            if (!equalsImpl(p.get(a), p.get(b))) {
                return false;
            }
        }

        return true;
    }

}
