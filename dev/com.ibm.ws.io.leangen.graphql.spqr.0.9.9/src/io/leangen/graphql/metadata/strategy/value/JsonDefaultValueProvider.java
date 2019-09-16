package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private final ValueMapper valueMapper;

    private static final Logger log = LoggerFactory.getLogger(JsonDefaultValueProvider.class);

    public JsonDefaultValueProvider(GlobalEnvironment environment) {
        this.valueMapper =  Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
                .getValueMapper(Collections.emptyMap(), environment);
    }

    @Override
    public Object getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, Object initialValue) {
        return valueMapper.fromString((String) initialValue, type);
    }
}
