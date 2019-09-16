package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.module.SimpleModule;

import java.util.Arrays;
import java.util.List;

public class JacksonModule implements SimpleModule {

    private static JsonNodeAdapter jsonNodeAdapter = new JsonNodeAdapter();
    private static JsonArrayAdapter jsonArrayAdapter = new JsonArrayAdapter();

    @Override
    public List<TypeMapper> getTypeMappers() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter, new JacksonObjectScalarMapper());
    }

    @Override
    public List<OutputConverter<?, ?>> getOutputConverters() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter);
    }

    @Override
    public List<InputConverter<?, ?>> getInputConverters() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter);
    }

    @Override
    public void setUp(SetupContext context) {
        if (!getTypeMappers().isEmpty()) {
            context.getSchemaGenerator().withTypeMappersPrepended(getTypeMappers().toArray(new TypeMapper[0]));
        }
        if (!getOutputConverters().isEmpty()) {
            context.getSchemaGenerator().withOutputConvertersPrepended(getOutputConverters().toArray(new OutputConverter[0]));
        }
        if (!getInputConverters().isEmpty()) {
            context.getSchemaGenerator().withInputConvertersPrepended(getInputConverters().toArray(new InputConverter[0]));
        }
        context.getSchemaGenerator().withTypeSynonymGroup(ObjectNode.class, POJONode.class);
        context.getSchemaGenerator().withTypeSynonymGroup(DecimalNode.class, NumericNode.class);
    }
}
