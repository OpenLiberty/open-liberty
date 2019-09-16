package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory, ScalarDeserializationStrategy {

    private final ObjectMapper prototype;
    private final List<Configurer> configurers;
    private final TypeInfoGenerator typeInfoGenerator;

    private static final Configurer IMPLICIT_MODULES = new ImplicitModuleConfigurer();

    @SuppressWarnings("WeakerAccess")
    public JacksonValueMapperFactory() {
        this(null, new DefaultTypeInfoGenerator(), defaultConfigurers());
    }

    private JacksonValueMapperFactory(ObjectMapper prototype, TypeInfoGenerator typeInfoGenerator, List<Configurer> configurers) {
        this.prototype = prototype;
        this.configurers = Objects.requireNonNull(configurers);
        this.typeInfoGenerator = Objects.requireNonNull(typeInfoGenerator);
    }

    @Override
    public JacksonValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        return new JacksonValueMapper(initBuilder(concreteSubTypes, environment));
    }

    private ObjectMapper initBuilder(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        ObjectMapper objectMapper = prototype != null ? prototype.copy() : new ObjectMapper();
        return this.configurers.stream().reduce(objectMapper, (mapper, configurer) ->
                configurer.configure(new ConfigurerParams(mapper, concreteSubTypes, this.typeInfoGenerator, environment)), (b1, b2) -> b2);
    }

    @Override
    public boolean isDirectlyDeserializable(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(TreeNode.class, type.getType());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class ImplicitModuleConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ConfigurerParams params) {
            return params.objectMapper.findAndRegisterModules();
        }
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ConfigurerParams params) {
            ObjectMapper mapper = params.objectMapper
                    .registerModule(getAnnotationIntrospectorModule(
                            unambiguousSubtypes(params.concreteSubTypes),
                            ambiguousSubtypes(params.concreteSubTypes, params.metaDataGen, params.environment.messageBundle),
                            params.environment.messageBundle));
            if (!params.environment.getInputConverters().isEmpty()) {
                mapper.registerModule(getDeserializersModule(params.environment));
            }
            return mapper;
        }

        private Map<Class, Class> unambiguousSubtypes(Map<Class, List<Class<?>>> concreteSubTypes) {
            return concreteSubTypes.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        }

        private Map<Type, List<NamedType>> ambiguousSubtypes(Map<Class, List<Class<?>>> concreteSubTypes, TypeInfoGenerator metaDataGen, MessageBundle messageBundle) {
            Map<Type, List<NamedType>> types = new HashMap<>();
            concreteSubTypes.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .forEach(entry -> {
                        List<NamedType> subTypes = entry.getValue().stream()
                                .map(sub -> new NamedType(sub, metaDataGen.generateTypeName(GenericTypeReflector.annotate(sub), messageBundle)))
                                .collect(Collectors.toList());
                        if (!subTypes.isEmpty()) {
                            types.put(entry.getKey(), subTypes);
                        }
                    });
            return types;
        }

        private Module getDeserializersModule(GlobalEnvironment environment) {
            return new Module() {
                @Override
                public String getModuleName() {
                    return "graphql-spqr-deserializers";
                }

                @Override
                public Version version() {
                    return Version.unknownVersion();
                }

                @Override
                public void setupModule(SetupContext setupContext) {
                    setupContext.addDeserializers(new ConvertingDeserializers(environment));
                }
            };
        }

        private Module getAnnotationIntrospectorModule(Map<Class, Class> unambiguousTypes, Map<Type, List<NamedType>> ambiguousTypes, MessageBundle messageBundle) {
            SimpleModule module = new SimpleModule("graphql-spqr-annotation-introspector") {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.insertAnnotationIntrospector(new AnnotationIntrospector(ambiguousTypes, messageBundle));
                }
            };
            unambiguousTypes.forEach(module::addAbstractTypeMapping);
            return module;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        ObjectMapper configure(ConfigurerParams params);
    }

    @SuppressWarnings("WeakerAccess")
    public static class ConfigurerParams {

        final ObjectMapper objectMapper;
        final Map<Class, List<Class<?>>> concreteSubTypes;
        final TypeInfoGenerator metaDataGen;
        final GlobalEnvironment environment;

        ConfigurerParams(ObjectMapper objectMapper, Map<Class, List<Class<?>>> concreteSubTypes, TypeInfoGenerator metaDataGen, GlobalEnvironment environment) {
            this.objectMapper = objectMapper;
            this.concreteSubTypes = concreteSubTypes;
            this.metaDataGen = metaDataGen;
            this.environment = environment;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public Map<Class, List<Class<?>>> getConcreteSubTypes() {
            return concreteSubTypes;
        }

        public TypeInfoGenerator getMetaDataGen() {
            return metaDataGen;
        }

        public GlobalEnvironment getEnvironment() {
            return environment;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public static class Builder {

        private List<Configurer> configurers = defaultConfigurers();
        private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
        private ObjectMapper prototype;

        public Builder withConfigurers(Configurer... configurer) {
            Collections.addAll(this.configurers, configurer);
            return this;
        }

        public Builder withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
            this.typeInfoGenerator = typeInfoGenerator;
            return this;
        }

        public Builder withPrototype(ObjectMapper prototype) {
            this.prototype = prototype;
            return this;
        }

        public Builder withExplicitModulesOnly() {
            this.configurers.remove(IMPLICIT_MODULES);
            return this;
        }

        public JacksonValueMapperFactory build() {
            return new JacksonValueMapperFactory(prototype, typeInfoGenerator, configurers);
        }
    }

    private static ArrayList<Configurer> defaultConfigurers() {
        return new ArrayList<>(Arrays.asList(IMPLICIT_MODULES, new AbstractClassAdapterConfigurer()));
    }
}
