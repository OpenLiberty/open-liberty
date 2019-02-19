package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputFieldInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JacksonValueMapper implements ValueMapper, InputFieldBuilder {

    private final ObjectMapper objectMapper;
    private final InputFieldInfoGenerator inputInfoGen = new InputFieldInfoGenerator();

    private static final Logger log = LoggerFactory.getLogger(JacksonValueMapper.class);

    JacksonValueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) {
        try {
            return objectMapper.convertValue(graphQLInput, objectMapper.getTypeFactory().constructType(outputType.getType()));
        } catch (IllegalArgumentException e) {
            throw new InputParsingException(graphQLInput, outputType.getType(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, AnnotatedType type) {
        if (json == null || String.class.equals(type.getType())) {
            return (T) json;
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (IOException e) {
            throw new InputParsingException(json, type.getType(), e);
        }
    }

    @Override
    public String toString(Object output) {
        if (output == null || output instanceof String) {
            return (String) output;
        }
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(params.getType().getType());
        BeanDescription desc = objectMapper.getDeserializationConfig().introspect(javaType);
        return desc.findProperties().stream()
                .filter(BeanPropertyDefinition::couldDeserialize)
                .flatMap(prop -> toInputField(params.getType(), prop, params.getEnvironment()))
                .collect(Collectors.toSet());
    }

    private Stream<InputField> toInputField(AnnotatedType type, BeanPropertyDefinition prop, GlobalEnvironment environment) {
        PropertyDescriptorFactory descFactory = new PropertyDescriptorFactory(type, environment.typeTransformer);
        AnnotatedParameter ctorParam = prop.getConstructorParameter();
        if (ctorParam != null) {
            return toInputField(descFactory.fromConstructorParameter(ctorParam), prop, objectMapper, environment);
        }
        if (prop.getSetter() != null) {
            return toInputField(descFactory.fromSetter(prop.getSetter()), prop, objectMapper, environment);
        }
        if (prop.getGetter() != null) {
            return toInputField(descFactory.fromGetter(prop.getGetter()), prop, objectMapper, environment);
        }
        if (prop.getField() != null) {
            return toInputField(descFactory.fromField(prop.getField()), prop, objectMapper, environment);
        }
        throw new TypeMappingException("Unknown input field mapping style encountered");
    }

    private Stream<InputField> toInputField(PropertyDescriptor desc, BeanPropertyDefinition prop, ObjectMapper objectMapper, GlobalEnvironment environment) {
        if (!environment.inclusionStrategy.includeInputField(desc.declaringClass, desc.element, desc.type)) {
            return Stream.empty();
        }

        AnnotatedType deserializableType = resolveDeserializableType(desc.accessor, desc.type, desc.accessor.getType(), objectMapper);
        Object defaultValue = defaultValue(desc.declaringType, prop, desc.type, environment.typeTransformer, environment);
        return Stream.of(new InputField(prop.getName(), prop.getMetadata().getDescription(), desc.type, deserializableType, defaultValue, desc.element));
    }

    private AnnotatedType resolveDeserializableType(Annotated accessor, AnnotatedType realType, JavaType baseType, ObjectMapper objectMapper) {
        AnnotationIntrospector introspector = objectMapper.getDeserializationConfig().getAnnotationIntrospector();
        try {
            objectMapper.getDeserializationContext().getFactory().mapAbstractType(objectMapper.getDeserializationConfig(), objectMapper.constructType(Map.class));
            JavaType refined = introspector.refineDeserializationType(objectMapper.getDeserializationConfig(), accessor, baseType);
            Class<?> raw = ClassUtils.getRawType(realType.getType());
            if (!refined.getRawClass().equals(raw)) {
                if (GenericTypeReflector.isSuperType(realType.getType(), refined.getRawClass())) {
                    AnnotatedType candidate = GenericTypeReflector.getExactSubType(realType, refined.getRawClass());
                    if (!ClassUtils.isMissingTypeParameters(candidate.getType())) {
                        return candidate;
                    }
                }
                return GenericTypeReflector.updateAnnotations(TypeUtils.toJavaType(refined), realType.getAnnotations());
            }
        } catch (JsonMappingException e) {
            /*no-op*/
        } catch (Exception e) {
            log.warn("Failed to determine the deserializable type for " + GenericTypeReflector.getTypeName(realType.getType())
                    + " due to an exception", e);
        }
        return realType;
    }

    protected Object defaultValue(AnnotatedType type, BeanPropertyDefinition prop, AnnotatedType fieldType, TypeTransformer typeTransformer, GlobalEnvironment environment) {
        List<AnnotatedElement> annotatedCandidates = new ArrayList<>(4);
        PropertyDescriptorFactory descFactory = new PropertyDescriptorFactory(type, typeTransformer);
        AnnotatedParameter ctorParam = prop.getConstructorParameter();
        if (ctorParam != null) {
            annotatedCandidates.add(descFactory.fromConstructorParameter(ctorParam).element);
        }
        if (prop.getSetter() != null) {
            annotatedCandidates.add(descFactory.fromSetter(prop.getSetter()).element);
        }
        if (prop.getGetter() != null) {
            annotatedCandidates.add(descFactory.fromGetter(prop.getGetter()).element);
        }
        if (prop.getField() != null) {
            annotatedCandidates.add(descFactory.fromField(prop.getField()).element);
        }
        return inputInfoGen.defaultValue(annotatedCandidates, fieldType, (element, t, val) -> fromString((String) val, t), environment).orElse(null);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    private static class PropertyDescriptorFactory {

        private final AnnotatedType type;
        private final TypeTransformer transformer;

        PropertyDescriptorFactory(AnnotatedType type, TypeTransformer typeTransformer) {
            this.type = type;
            this.transformer = typeTransformer;
        }

        PropertyDescriptor fromConstructorParameter(AnnotatedParameter ctorParam) {
            Executable constructor = (Executable) ctorParam.getOwner().getMember();
            Parameter parameter = constructor.getParameters()[ctorParam.getIndex()];
            AnnotatedType fieldType = transform(ClassUtils.getParameterTypes(constructor, type)[ctorParam.getIndex()], parameter);
            return new PropertyDescriptor(type, constructor.getDeclaringClass(), parameter, fieldType, ctorParam);
        }

        PropertyDescriptor fromSetter(AnnotatedMethod setterMethod) {
            Method setter = setterMethod.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getParameterTypes(setter, type)[0], setter, type);
            return new PropertyDescriptor(type, setter.getDeclaringClass(), setter, fieldType, setterMethod);
        }

        PropertyDescriptor fromGetter(AnnotatedMethod getterMethod) {
            Method getter = getterMethod.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getReturnType(getter, type), getter, type);
            return new PropertyDescriptor(type, getter.getDeclaringClass(), getter, fieldType, getterMethod);
        }

        PropertyDescriptor fromField(AnnotatedField fld) {
            Field field = fld.getAnnotated();
            AnnotatedType fieldType = transform(ClassUtils.getFieldType(field, type), field, type);
            return new PropertyDescriptor(type, field.getDeclaringClass(), field, fieldType, fld);
        }

        AnnotatedType transform(AnnotatedType type, Member member, AnnotatedType declaringType) {
            try {
                return transformer.transform(type);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(member, declaringType, e);
            }
        }

        AnnotatedType transform(AnnotatedType type, Parameter parameter) {
            try {
                return transformer.transform(type);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(parameter.getDeclaringExecutable(), parameter, e);
            }
        }
    }

    private static class PropertyDescriptor {

        final AnnotatedType declaringType;
        final AnnotatedElement element;
        final AnnotatedType type;
        final Class<?> declaringClass;
        final Annotated accessor;

        PropertyDescriptor(AnnotatedType declaringType, Class<?> declaringClass, AnnotatedElement element, AnnotatedType type, Annotated accessor) {
            this.declaringType = declaringType;
            this.element = element;
            this.type = type;
            this.declaringClass = declaringClass;
            this.accessor = accessor;
        }
    }
}
