package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.Method;

public class PropertyOperationNameGenerator extends AnnotatedOperationNameGenerator {

    @Override
    public String generateQueryName(OperationNameGeneratorParams<?> params) {
        String name = super.generateQueryName(params);
        if (Utils.isEmpty(name) && params.isMethod()) {
            return ClassUtils.getFieldNameFromGetter((Method) params.getElement());
        }
        return name;
    }
}
