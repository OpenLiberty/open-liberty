package io.leangen.graphql.generator.union;

import java.lang.reflect.AnnotatedType;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class Union2<T1, T2> extends Union {

    public Union2(String name, String description, List<AnnotatedType> javaTypes) {
        super(name, description, javaTypes);
    }
}
