/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams;

import org.eclipse.microprofile.reactive.streams.spi.Graph;

/**
 * Exists to allow access to the {@code toGraph} methods on each builder, so that these methods don't have to be
 * exposed publicly in the API.
 *
 * This is intended only for use by implementations of the API, to get direct access to the graphs without having to
 * build a publisher, processor or subscriber. This is particularly useful for cases where an implementation would
 * like to do additional manipulation using its own API to the stream, but have those manipulations fused to the
 * graph being manipulated.
 */
public class GraphAccessor {

  private GraphAccessor() {
  }

  /**
   * Build the graph for the given {@link PublisherBuilder}.
   *
   * @param publisherBuilder The builder to build the graph for.
   * @return The built graph.
   */
  public static Graph buildGraphFor(PublisherBuilder<?> publisherBuilder) {
    return publisherBuilder.toGraph();
  }

  /**
   * Build the graph for the given {@link ProcessorBuilder}.
   *
   * @param processorBuilder The builder to build the graph for.
   * @return The built graph.
   */
  public static Graph buildGraphFor(ProcessorBuilder<?, ?> processorBuilder) {
    return processorBuilder.toGraph();
  }

  /**
   * Build the graph for the given {@link SubscriberBuilder}.
   *
   * @param subscriberBuilder The builder to build the graph for.
   * @return The built graph.
   */
  public static Graph buildGraphFor(SubscriberBuilder<?, ?> subscriberBuilder) {
    return subscriberBuilder.toGraph();
  }

  /**
   * Build the graph for the given {@link CompletionRunner}.
   *
   * @param completionRunner The runner to build the graph for.
   * @return The built graph.
   */
  public static Graph buildGraphFor(CompletionRunner<?> completionRunner) {
    return completionRunner.toGraph();
  }

}
