package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.Ignore;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.DirectiveArgument;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
@Ignore
public class NonNullMapper implements TypeMapper, Comparator<AnnotatedType>, SchemaTransformer {

    private final Set<Class<? extends Annotation>> nonNullAnnotations;

    private static final Logger log = LoggerFactory.getLogger(NonNullMapper.class);

    private static final String[] COMMON_NON_NULL_ANNOTATIONS = {
            "javax.annotation.Nonnull",
            "javax.validation.constraints.NotNull",
            "javax.validation.constraints.NotEmpty",
            "javax.validation.constraints.NotBlank"
    };

    @SuppressWarnings("unchecked")
    public NonNullMapper() {
        Set<Class<? extends Annotation>> annotations = new HashSet<>();
        annotations.add(io.leangen.graphql.annotations.GraphQLNonNull.class);
        for (String additional : COMMON_NON_NULL_ANNOTATIONS) {
            try {
                annotations.add((Class<? extends Annotation>) ClassUtils.forName(additional));
            } catch (ClassNotFoundException e) {
                /*no-op*/
            }
        }
        this.nonNullAnnotations = Collections.unmodifiableSet(annotations);
    }

    @Override
    public GraphQLNonNull toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        mappersToSkip.add(this.getClass());
        GraphQLOutputType inner = operationMapper.toGraphQLType(javaType, mappersToSkip, buildContext);
        return inner instanceof GraphQLNonNull ? (GraphQLNonNull) inner : new GraphQLNonNull(inner);
    }

    @Override
    public GraphQLNonNull toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        mappersToSkip.add(this.getClass());
        GraphQLInputType inner = operationMapper.toGraphQLInputType(javaType, mappersToSkip, buildContext);
        return inner instanceof GraphQLNonNull ? (GraphQLNonNull) inner : new GraphQLNonNull(inner);
    }

    @Override
    public GraphQLFieldDefinition transformField(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        if (shouldWrap(field.getType(), operation.getTypedElement())) {
            return field.transform(builder -> builder.type(new GraphQLNonNull(field.getType())));
        }
        return field;
    }

    @Override
    public GraphQLInputObjectField transformInputField(GraphQLInputObjectField field, InputField inputField, OperationMapper operationMapper, BuildContext buildContext) {
        if (field.getDefaultValue() == null && shouldWrap(field.getType(), inputField.getTypedElement())) {
            return field.transform(builder -> builder.type(new GraphQLNonNull(field.getType())));
        }
        if (shouldUnwrap(field.getDefaultValue(), field.getType())) {
            //do not warn on primitives as their non-nullness is implicit
            if (!ClassUtils.getRawType(inputField.getJavaType().getType()).isPrimitive()) {
                log.warn("Non-null input field with a default value will be treated as nullable: " + inputField);
            }
            return field.transform(builder -> builder.type((GraphQLInputType) GraphQLUtils.unwrapNonNull(field.getType())));
        }
        return field;
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, OperationArgument operationArgument, OperationMapper operationMapper, BuildContext buildContext) {
        return transformArgument(argument, operationArgument.getTypedElement(), operationArgument.toString(), operationMapper, buildContext);
    }

    @Override
    public GraphQLArgument transformArgument(GraphQLArgument argument, DirectiveArgument directiveArgument, OperationMapper operationMapper, BuildContext buildContext) {
        if (directiveArgument.getAnnotation() != null && directiveArgument.getDefaultValue() == null) {
            return argument.transform(builder -> builder.type(GraphQLNonNull.nonNull(argument.getType())));
        }
        return transformArgument(argument, directiveArgument.getTypedElement(), directiveArgument.toString(), operationMapper, buildContext);
    }

    private GraphQLArgument transformArgument(GraphQLArgument argument, TypedElement element, String description, OperationMapper operationMapper, BuildContext buildContext) {
        if (argument.getDefaultValue() == null && shouldWrap(argument.getType(), element)) {
            return argument.transform(builder -> builder.type(new GraphQLNonNull(argument.getType())));
        }
        if (shouldUnwrap(argument.getDefaultValue(), argument.getType())) {
            //do not warn on primitives as their non-nullness is implicit
            if (!ClassUtils.getRawType(element.getJavaType().getType()).isPrimitive()) {
                log.warn("Non-null argument with a default value will be treated as nullable: " + description);
            }
            return argument.transform(builder -> builder.type((GraphQLInputType) GraphQLUtils.unwrapNonNull(argument.getType())));
        }
        return argument;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return nonNullAnnotations.stream().anyMatch(type::isAnnotationPresent) || ClassUtils.getRawType(type.getType()).isPrimitive();
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        return ClassUtils.removeAnnotations(o1, nonNullAnnotations).equals(ClassUtils.removeAnnotations(o2, nonNullAnnotations)) ? 0 : -1;
    }

    private boolean shouldWrap(GraphQLType type, TypedElement typedElement) {
        return !(type instanceof GraphQLNonNull) && nonNullAnnotations.stream().anyMatch(typedElement::isAnnotationPresent);
    }

    private boolean shouldUnwrap(Object defaultValue, GraphQLType type) {
        return defaultValue != null && type instanceof GraphQLNonNull;
    }
}
