package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

public class JsonArrayAdapter extends AbstractTypeSubstitutingMapper<List<JsonNode>> implements InputConverter<ArrayNode, List<JsonNode>>, OutputConverter<ArrayNode, List> {

    private static final AnnotatedType JSON = GenericTypeReflector.annotate(JsonNode.class);

    @Override
    public ArrayNode convertInput(List<JsonNode> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return new ArrayNode(JsonNodeFactory.instance, substitute);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.annotate(new TypeToken<List<JsonNode>>(){}.getType(), original.getAnnotations());
    }

    @Override
    public List convertOutput(ArrayNode original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        List<Object> nodes = new ArrayList<>(original.size());
        for (JsonNode element : original) {
            nodes.add(resolutionEnvironment.convertOutput(element, JSON));
        }
        return nodes;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(ArrayNode.class, type.getType());
    }
}
