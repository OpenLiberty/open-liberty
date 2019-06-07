package io.leangen.graphql.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bojan Tomic (kaqqao)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Interface {

    boolean implementationAutoDiscovery() default false;

    String[] scanPackages() default {};

    String[] fieldOrder() default {};
}
