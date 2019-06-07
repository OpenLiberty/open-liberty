package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ArgumentInjector {
    
    Object getArgumentValue(ArgumentInjectorParams params);

    boolean supports(AnnotatedType type, Parameter parameter);
}
