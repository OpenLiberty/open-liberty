package io.smallrye.reactive.messaging.annotations;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier used with {@code @Inject} to retrieve a {@code Publisher} managed by the Reactive Messaging implementation.
 * <p>
 * Can be injected:
 * <ul>
 * <li>Publisher<X> with X the payload type</li>
 * <li>Publisher<Message<X>> with X the payload type</li>
 * <li>Flowable<X> with X the payload type</li>
 * <li>Flowable<Message<X>> with X the payload type</li>
 * <li>PublisherBuilder<Message<X>> with X the payload type</li>
 * <li>PublisherBuilder<X> with X the payload type</li>
 * </ul>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Stream {

  /**
   * The name of the stream (indicated in the {@code @Outgoing} annotation.
   *
   * @return the stream name, mandatory, non null and non-blank. It must matches one of the available streams.
   */
  @Nonbinding
  String value();
}
