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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.annotation.Trivial;

import java.util.Optional;

/**
 * Utility class for copying model objects
 */
public class ModelCopy {

    /**
     * Copy an object from an OpenAPI model
     * <p>
     * {@code object} and any objects it contains should be MP OpenAPI model objects, maps, lists or immutable objects (which won't be copied)
     * 
     * @param object the object to copy
     * @return the copy
     */
    public static Object copy(Object object) {
        return doCopy(object);
    }
    
    @Trivial
    private static Object doCopy(Object object) {
        if (object == null) {
            return null;
        }
        Optional<ModelType> modelObject = ModelType.getModelObject(object.getClass());
        if (modelObject.isPresent()) {
            return copyModelObject(modelObject.get(), object);
        } else if (object instanceof List) {
            return copyList((List<?>) object);
        } else if (object instanceof Map) {
            return copyMap((Map<?, ?>) object);
        } else {
            return object; // Assume non-collection, non-model objects referenced are immutable
        }
    }

    @Trivial
    private static Object copyList(List<?> copyFrom) {
        List<Object> copyTo = new ArrayList<>();
        for (Object o : copyFrom) {
            copyTo.add(doCopy(o));
        }
        return copyTo;
    }

    @Trivial
    private static Map<Object, Object> copyMap(Map<?, ?> copyFrom) {
        Map<Object, Object> copyTo = new HashMap<>();
        for (Entry<?, ?> entry : copyFrom.entrySet()) {
            copyTo.put(entry.getKey(), doCopy(entry.getValue()));
        }
        return copyTo;
    }

    @Trivial
    private static Object copyModelObject(ModelType mo, Object object) {
        Object result = mo.createInstance();
        for (ModelType.ModelParameter desc : mo.getParameters()) {
            desc.set(result, doCopy(desc.get(object)));
        }
        return result;
    }

}
