package io.openliberty.microprofile.openapi20.merge;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public class ModelEquality {

    public static boolean equals(Object a, Object b) {
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

    private static boolean equalsMap(Map<?, ?> a, Map<?, ?> b) {
        if (!Objects.equals(a.keySet(), b.keySet())) {
            return false;
        }

        for (Entry<?, ?> entry : a.entrySet()) {
            if (!equals(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }

    private static boolean equalsList(List<?> a, List<?> b) {
        if (a.size() != b.size()) {
            return false;
        }

        Iterator<?> ai = a.iterator();
        Iterator<?> bi = b.iterator();
        while (ai.hasNext()) {
            if (!equals(ai.next(), bi.next())) {
                return false;
            }
        }

        return true;
    }

    private static boolean equalsModelObject(ModelType modelType, Object a, Object b) {
        if (!modelType.isInstance(b)) {
            return false;
        }

        for (ModelType.ModelParameter p : modelType.getParameters()) {
            if (!equals(p.get(a), p.get(b))) {
                return false;
            }
        }

        return true;
    }

}
