package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArgumentInjectorRegistry {
    
    private final List<ArgumentInjector> argumentInjectors;

    public ArgumentInjectorRegistry(List<ArgumentInjector> argumentInjectors) {
        this.argumentInjectors = Collections.unmodifiableList(argumentInjectors);
    }

    public ArgumentInjector getInjector(AnnotatedType inputType, Parameter parameter) {
        return argumentInjectors.stream().filter(injector -> injector.supports(inputType, parameter)).findFirst().orElse(null);
    }
}
