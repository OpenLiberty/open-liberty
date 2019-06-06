package io.smallrye.reactive.messaging;

public class ProcessingException extends RuntimeException {
  public ProcessingException(String method, Throwable cause) {
    super("Exception thrown when calling the method " + method, cause);
  }
}
