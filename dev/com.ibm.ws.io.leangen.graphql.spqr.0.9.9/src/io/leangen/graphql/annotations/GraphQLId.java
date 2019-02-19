package io.leangen.graphql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes the annotated element is to be mapped as a GraphQL ID
 *
 * NOTE:
 * Due to the lack of support for {@code AnnotatedType} in <i>all</i> JSON libraries for Java,
 * {@link ElementType#TYPE_USE} annotations on input field types or nested operation argument types are lost.
 * Thus, such annotations can only safely be used on top-level argument or output types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE_USE, ElementType.FIELD, ElementType.TYPE})
public @interface GraphQLId {
    String RELAY_ID_FIELD_NAME = "id"; //The name of the ID field, as defined by the Node interface
    
    boolean relayId() default false;
}
