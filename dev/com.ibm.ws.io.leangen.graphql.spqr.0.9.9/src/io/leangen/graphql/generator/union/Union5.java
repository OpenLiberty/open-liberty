package io.leangen.graphql.generator.union;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class Union5<T1, T2, T3, T4, T5> extends Union {

    public Union5(String name, String description, List<AnnotatedType> javaTypes) {
        super(name, description, javaTypes);
    }
}
