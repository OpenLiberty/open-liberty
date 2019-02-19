package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter representing a value injected from the current execution environment.
 * Currently, the annotated parameter is allowed to be of the following types:
 * <ol>
 * <li>{@code Set<String>} - Injects the list of names of requested direct sub-fields</li>
 * <li>{@link graphql.language.Field} - Injects the AST {@link graphql.language.Field} currently being resolved</li>
 * <li>{@code List<Field>} - Injects all the AST {@link graphql.language.Field}s on the current level</li>
 * <li>{@link io.leangen.graphql.metadata.strategy.value.ValueMapper} - Injects a {@link io.leangen.graphql.metadata.strategy.value.ValueMapper} appropriate for the current resolver</li>
 * <li>{@link io.leangen.graphql.execution.ResolutionEnvironment} - Injects the entire {@link io.leangen.graphql.execution.ResolutionEnvironment}</li>
 * </ol>
 */
@Ignore
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Info {
}
