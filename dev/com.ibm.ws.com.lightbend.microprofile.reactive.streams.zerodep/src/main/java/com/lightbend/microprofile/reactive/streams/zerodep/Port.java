/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.streams.zerodep;

/**
 * A port, which may sit between two stages of this graph.
 */
interface Port {
  /**
   * If an exception is thrown by the graph, or otherwise encountered, each port will be shut down in the order they
   * were created, by invoking this. This method should implement any clean up associated with a port, if the port
   * isn't already shut down.
   */
  void onStreamFailure(Throwable reason);

  /**
   * Verify that this port is ready to start receiving signals.
   */
  void verifyReady();
}
