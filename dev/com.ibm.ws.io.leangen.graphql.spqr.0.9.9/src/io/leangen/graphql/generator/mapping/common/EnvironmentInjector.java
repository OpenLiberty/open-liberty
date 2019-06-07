package io.leangen.graphql.generator.mapping.common;

import graphql.language.Field;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.Info;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class EnvironmentInjector implements ArgumentInjector {
    
    private static final Type listOfFields = new TypeToken<List<Field>>(){}.getType();
    private static final Type setOfStrings = new TypeToken<Set<String>>(){}.getType();
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        if (GenericTypeReflector.isSuperType(setOfStrings, params.getType().getType())) {
            return params.getResolutionEnvironment().dataFetchingEnvironment.getSelectionSet().get().keySet();
        }
        Class raw = GenericTypeReflector.erase(params.getType().getType());
        if (Field.class.equals(raw)) {
            return params.getResolutionEnvironment().fields.get(0);
        }
        if (GenericTypeReflector.isSuperType(listOfFields, params.getType().getType())) {
            return params.getResolutionEnvironment().fields;
        }
        if (ValueMapper.class.isAssignableFrom(raw)) {
            return params.getResolutionEnvironment().valueMapper;
        }
        if (ResolutionEnvironment.class.isAssignableFrom(raw)) {
            return params.getResolutionEnvironment();
        }
        throw new IllegalArgumentException("Argument of type " + raw.getName() 
                + " can not be injected via @" + Info.class.getSimpleName());
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(Info.class);
    }
}
