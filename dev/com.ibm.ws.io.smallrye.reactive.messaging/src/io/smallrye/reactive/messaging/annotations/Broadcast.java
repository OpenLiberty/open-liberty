package io.smallrye.reactive.messaging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configure if the annotated publisher should dispatch the messages to several subscribers.
 *
 * Experimental !
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Broadcast {

  /**
   * Indicates the number of subscribers required before dispatching the items.
   * @return the value, 0 indicates immediate.
   */
  int value() default 0;

}
