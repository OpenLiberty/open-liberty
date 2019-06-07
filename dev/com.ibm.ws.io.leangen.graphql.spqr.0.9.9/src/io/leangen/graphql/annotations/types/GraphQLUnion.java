package io.leangen.graphql.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphQLUnion {

    String name();

    String description() default "";

    Class<?>[] possibleTypes() default {};

    Class<? extends PossibleTypeFactory> possibleTypeFactory() default DummyPossibleTypeFactory.class;

    boolean possibleTypeAutoDiscovery() default false;

    String[] scanPackages() default {};

    interface PossibleTypeFactory {
        List<AnnotatedType> getPossibleTypes();
    }

    class DummyPossibleTypeFactory implements PossibleTypeFactory {
        @Override
        public List<AnnotatedType> getPossibleTypes() {
            return Collections.emptyList();
        }
    }
}
