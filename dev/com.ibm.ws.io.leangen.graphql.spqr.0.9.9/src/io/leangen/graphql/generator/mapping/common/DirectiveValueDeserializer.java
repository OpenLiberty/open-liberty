package io.leangen.graphql.generator.mapping.common;

import graphql.introspection.Introspection;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLDirective;
import io.leangen.graphql.execution.Directives;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.introspection.Introspection.DirectiveLocation.FIELD;
import static graphql.introspection.Introspection.DirectiveLocation.FRAGMENT_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.FRAGMENT_SPREAD;
import static graphql.introspection.Introspection.DirectiveLocation.INLINE_FRAGMENT;
import static graphql.introspection.Introspection.DirectiveLocation.MUTATION;
import static graphql.introspection.Introspection.DirectiveLocation.QUERY;

public class DirectiveValueDeserializer implements ArgumentInjector {

    private static final Introspection.DirectiveLocation[] SORTED_LOCATIONS = {FIELD, INLINE_FRAGMENT, FRAGMENT_SPREAD, FRAGMENT_DEFINITION, QUERY, MUTATION};

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        GraphQLDirective descriptor = params.getParameter().getAnnotation(GraphQLDirective.class);
        boolean allDirectives = GenericTypeReflector.isSuperType(Collection.class, params.getType().getType());
        ResolutionEnvironment env = params.getResolutionEnvironment();
        String fallBackDirectiveName = env.globalEnvironment.typeInfoGenerator.generateDirectiveTypeName(params.getBaseType(), env.globalEnvironment.messageBundle);
        String directiveName = Utils.coalesce(descriptor.name(), fallBackDirectiveName);
        Stream<Introspection.DirectiveLocation> locations = descriptor.locations().length != 0
                ? Arrays.stream(descriptor.locations())
                : sortedLocations(params.getResolutionEnvironment().dataFetchingEnvironment.getGraphQLSchema().getDirective(directiveName).validLocations());
        Directives directives = env.getDirectives();
        Stream<Map<String, Object>> rawValues = locations
                .map(loc -> directives.find(loc, directiveName))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream);
        Object deserializableValue = allDirectives ? rawValues.collect(Collectors.toList()) : rawValues.findFirst().orElse(null);
        if (deserializableValue == null) {
            return null;
        }
        return params.getResolutionEnvironment().valueMapper.fromInput(deserializableValue, params.getType());
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(GraphQLDirective.class);
    }

    private Stream<Introspection.DirectiveLocation> sortedLocations(Set<Introspection.DirectiveLocation> locations) {
        return Arrays.stream(SORTED_LOCATIONS)
                .map(loc -> locations.contains(loc) ? loc : null)
                .filter(Objects::nonNull);
    }
}
