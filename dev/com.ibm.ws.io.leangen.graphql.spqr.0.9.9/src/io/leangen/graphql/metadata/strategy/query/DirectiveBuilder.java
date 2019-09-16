package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.Directive;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

public interface DirectiveBuilder {

    default List<Directive> buildSchemaDirectives(AnnotatedType schemaDescriptorType, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildObjectTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildScalarTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildFieldDefinitionDirectives(AnnotatedElement element, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildArgumentDefinitionDirectives(AnnotatedElement element, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildInterfaceTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildUnionTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildEnumTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildEnumValueDirectives(Enum<?> value, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildInputObjectTypeDirectives(AnnotatedType type, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default List<Directive> buildInputFieldDefinitionDirectives(AnnotatedElement element, DirectiveBuilderParams params) {
        return Collections.emptyList();
    }

    default Directive buildClientDirective(AnnotatedType directiveType, DirectiveBuilderParams params) {
        return null;
    }
}
