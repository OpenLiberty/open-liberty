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

/**
 * MicroProfile Reactive Streams Operators.
 * <p>
 * This provides operators for building stream graphs that consume or produce elements using the
 * {@link org.reactivestreams.Publisher}, {@link org.reactivestreams.Subscriber} and
 * {@link org.reactivestreams.Processor} interfaces.
 * <p>
 * There are four primary classes used for building these graphs:
 * <p>
 * <ul>
 * <li>{@link org.eclipse.microprofile.reactive.streams.PublisherBuilder}, which when built produces a
 * {@link org.reactivestreams.Publisher}</li>
 * <li>{@link org.eclipse.microprofile.reactive.streams.SubscriberBuilder}, which when built produces a
 * {@link org.eclipse.microprofile.reactive.streams.CompletionSubscriber}</li>
 * <li>{@link org.eclipse.microprofile.reactive.streams.ProcessorBuilder}, which when built produces a
 * {@link org.reactivestreams.Processor}</li>
 * <li>{@link org.eclipse.microprofile.reactive.streams.CompletionRunner}, which when built produces a
 * {@link java.util.concurrent.CompletionStage}</li>
 * </ul>
 * <p>
 * Operations on these builders may change the shape of the builder, for example,
 * {@link org.eclipse.microprofile.reactive.streams.ProcessorBuilder#toList()} changes the builder to a
 * {@link org.eclipse.microprofile.reactive.streams.SubscriberBuilder}, since the processor now has a termination
 * stage to direct its elements to.
 * <p>
 * {@link SubscriberBuilder}'s are a special case, in that they don't just build a
 * {@link org.reactivestreams.Subscriber}, they build a
 * {@link org.eclipse.microprofile.reactive.streams.CompletionSubscriber}, which encapsulates both a
 * {@link org.reactivestreams.Subscriber} and a {@link java.util.concurrent.CompletionStage} of the result of the
 * subscriber processing. This {@link java.util.concurrent.CompletionStage} will be redeemed with a result when the
 * stream terminates normally, or if the stream terminates with an error, will be redeemed with an error. The result is
 * specific to whatever the {@link org.reactivestreams.Subscriber} does, for example, in the case of
 * {@link org.eclipse.microprofile.reactive.streams.ProcessorBuilder#toList()}, the result will be a
 * {@link java.util.List} of all the elements produced by the {@link org.reactivestreams.Processor}, while in the case
 * of {@link org.eclipse.microprofile.reactive.streams.ProcessorBuilder#findFirst()}, it's an
 * {@link java.util.Optional} of the first element of the stream. In some cases, there is no result, in which case the
 * result is the {@link java.lang.Void} type, and the {@link java.util.concurrent.CompletionStage} is only useful for
 * signalling normal or error termination of the stream.
 * <p>
 * The {@link org.eclipse.microprofile.reactive.streams.CompletionRunner} builds a closed graph, in that case both a
 * {@link org.reactivestreams.Publisher} and {@link org.reactivestreams.Subscriber} have been provided, and building the
 * graph will run it and return the result of the {@link org.reactivestreams.Subscriber} as a
 * {@link java.util.concurrent.CompletionStage}.
 * <p>
 * An example use of this API is perhaps you have a {@link org.reactivestreams.Publisher} of rows from a database,
 * and you want to output it as a comma separated list of lines to publish to an HTTP client request body, which
 * expects a {@link org.reactivestreams.Publisher} of {@link java.nio.ByteBuffer}. Here's how this might be
 * implemented:
 * <p>
 * <pre>
 *   Publisher&lt;Row&gt; rowsPublisher = ...;
 *
 *   Publisher&lt;ByteBuffer&gt; bodyPublisher =
 *     // Create a publisher builder from the rows publisher
 *     ReactiveStreams.fromPublisher(rowsPublisher)
 *       // Map the rows to CSV strings
 *       .map(row -&gt;
 *         String.format("%s, %s, %d\n", row.getString("firstName"),
 *           row.getString("lastName"), row.getInt("age))
 *       )
 *       // Convert to ByteBuffer
 *       .map(line -&gt; ByteBuffer.wrap(line.getBytes("utf-8")))
 *       // Build the publisher
 *       .build();
 *
 *   // Make the request
 *   HttpClient client = HttpClient.newHttpClient();
 *   client.send(
 *     HttpRequest
 *       .newBuilder(new URI("http://www.foo.com/"))
 *       .POST(BodyProcessor.fromPublisher(bodyPublisher))
 *       .build()
 *   );
 * </pre>
 */
package org.eclipse.microprofile.reactive.streams;
