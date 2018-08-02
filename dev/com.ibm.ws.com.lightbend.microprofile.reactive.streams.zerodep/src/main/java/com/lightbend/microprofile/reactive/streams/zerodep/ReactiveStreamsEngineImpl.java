/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.microprofile.reactive.streams.zerodep;

import org.eclipse.microprofile.reactive.streams.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.spi.UnsupportedStageException;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

/**
 * Implementation of the reactive streams engine.
 */
public class ReactiveStreamsEngineImpl implements ReactiveStreamsEngine {
  @Override
  public <T> Publisher<T> buildPublisher(Graph graph) throws UnsupportedStageException {
    return BuiltGraph.buildPublisher(ForkJoinPool.commonPool(), graph);
  }

  @Override
  public <T, R> CompletionSubscriber<T, R> buildSubscriber(Graph graph) throws UnsupportedStageException {
    return BuiltGraph.buildSubscriber(ForkJoinPool.commonPool(), graph);
  }

  @Override
  public <T, R> Processor<T, R> buildProcessor(Graph graph) throws UnsupportedStageException {
    return BuiltGraph.buildProcessor(ForkJoinPool.commonPool(), graph);
  }

  @Override
  public <T> CompletionStage<T> buildCompletion(Graph graph) throws UnsupportedStageException {
    return BuiltGraph.buildCompletion(ForkJoinPool.commonPool(), graph);
  }
}
