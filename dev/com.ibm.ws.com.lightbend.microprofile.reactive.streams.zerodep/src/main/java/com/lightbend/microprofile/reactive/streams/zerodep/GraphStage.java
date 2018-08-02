/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.eclipse.microprofile.reactive.streams.spi.Graph;

import java.util.concurrent.Executor;

/**
 * Superclass of all graph stages.
 */
abstract class GraphStage {

  private final BuiltGraph builtGraph;

  GraphStage(BuiltGraph builtGraph) {
    this.builtGraph = builtGraph;
  }

  /**
   * Create a sub inlet for the given graph.
   * <p>
   * After being created, the inlet should have an inlet listener attached to it, and then it should be started.
   *
   * @param graph The graph.
   * @return The inlet.
   */
  protected <T> BuiltGraph.SubStageInlet<T> createSubInlet(Graph graph) {
    return builtGraph.buildSubInlet(graph);
  }

  protected Executor executor() {
    return builtGraph;
  }

  /**
   * Run a callback after the graph has started.
   */
  protected void postStart() {
    // Do nothing by default
  }

}
