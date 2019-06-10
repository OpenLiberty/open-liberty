package io.smallrye.reactive.messaging.annotations;

/**
 * Interface used to feed a stream from an <em>imperative</em> piece of code.
 * <p>
 * Instances are injected using:
 * <pre>
 * &#64;Inject @Stream("my-stream") Emitter&lt;String&gt; emitter;
 * </pre>
 * <p>
 * You can inject emitter sending payload or {@link org.eclipse.microprofile.reactive.messaging.Message Messages}.
 * <p>
 * The name of the stream (given in the {@link Stream Stream annotation}) indicates which streams is fed. If must match the name used in a
 * method using {@link org.eclipse.microprofile.reactive.messaging.Incoming @Incoming} or an outgoing stream configured in the
 * application configuration.
 *
 * @param <T> type of payload or {@link org.eclipse.microprofile.reactive.messaging.Message Message}.
 */
public interface Emitter<T> {

  /**
   * Sends a payload or a message to the stream.
   *
   * @param msg the <em>thing</em> to send, must not be {@code null}
   * @return the current emitter
   * @throws IllegalStateException if the stream does not have any pending requests, or if the stream has been
   *                               cancelled or terminated.
   */
  Emitter<T> send(T msg);

  /**
   * Completes the stream.
   * This method sends the completion signal, no messages can be sent once this method is called.
   */
  void complete();

  /**
   * Propagates an error in the stream.
   * This methods sends an error signal, no messages can be sent once this method is called.
   *
   * @param e the exception, must not be {@code null}
   */
  void error(Exception e);

  /**
   * @return {@code true} if the emitter has been terminated or the subscription cancelled.
   */
  boolean isCancelled();

  /**
   * @return {@code true} if the subscriber accepts messages, {@code false} otherwise.
   * Using {@link #send(Object)} on an emitter not expecting message would throw an {@link IllegalStateException}.
   */
  boolean isRequested();

}
