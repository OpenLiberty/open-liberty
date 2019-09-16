package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;

import java.util.List;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ValueMapperFactory {
    
    ValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment);
}
