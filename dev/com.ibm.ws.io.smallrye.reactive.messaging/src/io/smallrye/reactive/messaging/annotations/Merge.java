package io.smallrye.reactive.messaging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Temporary annotation - must be copied to spec.
 * <p>
 * Indicate the an {@code @Incoming} is connected to several upstream sources and merge the content according to the given policy.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge {

  enum Mode {
    /**
     * Pick the first source and use only this one.
     */
    ONE,
    /**
     * Merge the different sources. This strategy emits the items as they come.
     */
    MERGE,
    /**
     * Concat the sources.
     */
    CONCAT
  }

  Mode value() default Mode.MERGE;

}
