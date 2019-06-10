package io.smallrye.reactive.messaging;

public class WeavingException extends RuntimeException {

  public WeavingException(String source, String method, int number) {
    super("Unable to connect stream `" + source + "` (" + method + ") - several publishers are available (" + number + "), " +
      "use the @Merge annotation to indicate the merge strategy.");
  }

  /**
   * Used when a synchronous error is caught during the subscription
   * @param cause the cause
   */
  public WeavingException(String source, Throwable cause) {
    super("Synchronous error caught during the subscription of `" + source + "`", cause);
  }

  public WeavingException(String message) {
    super(message);
  }
}
