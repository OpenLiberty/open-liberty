package io.leangen.graphql.annotations;

import graphql.introspection.Introspection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Ignore
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLDirective {

    String name() default "";
    Introspection.DirectiveLocation[] locations() default {};
}
