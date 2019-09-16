package io.leangen.graphql.execution.complexity;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Utils;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;

public class JavaScriptEvaluator implements ComplexityFunction {
    
    private final ScriptEngine engine;

    public JavaScriptEvaluator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    @Override
    public int getComplexity(ResolvedField node, int childScore) {
        Resolver resolver = node.getResolver();
        if (resolver == null || Utils.isEmpty(resolver.getComplexityExpression())) {
            GraphQLType fieldType = node.getFieldType();
            if (fieldType instanceof GraphQLScalarType || fieldType instanceof GraphQLEnumType) {
                return 1;
            }
            if (GraphQLUtils.isRelayConnectionType(fieldType)) {
                Integer pageSize = getPageSize(node.getArguments());
                if (pageSize != null) {
                    return pageSize * childScore;
                }
            }
            return 1 + childScore;
        }
        Bindings bindings = engine.createBindings();
        bindings.putAll(node.getArguments());
        bindings.put("childScore", childScore);
        try {
            return ((Number) engine.eval(resolver.getComplexityExpression(), bindings)).intValue();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Complexity expression \"%s\" on field %s could not be evaluated",
                    resolver.getComplexityExpression(), node.getName()), e);
        }
    }

    private Integer getPageSize(Map<String, Object> arguments) {
        Object size = arguments.get("first");
        if (size instanceof Integer) {
            return (Integer) size;
        }
        size = arguments.get("last");
        if (size instanceof Integer) {
            return (Integer) size;
        }
        return null;
    }
}
