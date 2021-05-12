package io.openliberty.microprofile.openapi20.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private static Object copyList(List<?> copyFrom) {
        List<Object> copyTo = new ArrayList<>();
        for (Object o : copyFrom) {
            copyTo.add(copy(o));
        }
        return copyTo;
    }

    private static Map<Object, Object> copyMap(Map<?, ?> copyFrom) {
        Map<Object, Object> copyTo = new HashMap<>();
        for (Entry<?, ?> entry : copyFrom.entrySet()) {
            copyTo.put(entry.getKey(), copy(entry.getValue()));
        }
        return copyTo;
    }

    private static Object copyModelObject(ModelType mo, Object object) {
        Object result = mo.createInstance();
        for (ModelType.ModelParameter desc : mo.getParameters()) {
            desc.set(result, copy(desc.get(object)));
        }
        return result;
    }

}
