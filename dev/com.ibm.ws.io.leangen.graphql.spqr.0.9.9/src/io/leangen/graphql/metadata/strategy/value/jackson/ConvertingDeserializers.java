package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.ReferenceType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;

import java.lang.reflect.AnnotatedType;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConvertingDeserializers extends Deserializers.Base {

    private final Map<InputConverter, JsonDeserializer> deserializers;

    ConvertingDeserializers(GlobalEnvironment environment) {
        this.deserializers = environment.getInputConverters().stream()
                .collect(Collectors.toMap(Function.identity(), converter -> new ConvertingDeserializer(converter, environment)));
    }

    @Override
    public JsonDeserializer<?> findMapDeserializer(MapType type, DeserializationConfig config,
                                                   BeanDescription beanDesc, KeyDeserializer keyDeserializer,
                                                   TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) {

        return forJavaType(type);
    }

    @Override
    public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config, BeanDescription beanDesc) {
        return forType(GenericTypeReflector.annotate(type));
    }

    @Override
    public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType, DeserializationConfig config, BeanDescription beanDesc) {
        return forType(GenericTypeReflector.annotate(nodeType));
    }

    @Override
    public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
        return forJavaType(refType);
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) {
        return forJavaType(type);
    }

    @Override
    public JsonDeserializer<?> findArrayDeserializer(ArrayType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public JsonDeserializer<?> findCollectionDeserializer(CollectionType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config, BeanDescription beanDesc, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    @Override
    public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type, DeserializationConfig config, BeanDescription beanDesc, KeyDeserializer keyDeserializer, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) {
        return forJavaType(type);
    }

    private JsonDeserializer forType(AnnotatedType type) {
        return deserializers.keySet().stream().filter(c -> c.supports(type)).findFirst().map(deserializers::get).orElse(null);
    }

    private JsonDeserializer forJavaType(JavaType type) {
        return forType(TypeUtils.toJavaType(type));
    }
}
