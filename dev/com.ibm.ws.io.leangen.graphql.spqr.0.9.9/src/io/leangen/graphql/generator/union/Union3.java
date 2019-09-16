package io.leangen.graphql.generator.union;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class Union3<T1, T2, T3> extends Union {

    public Union3(String name, String description, List<AnnotatedType> javaTypes) {
        super(name, description, javaTypes);
    }
}
