/**
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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
 */
package org.eclipse.microprofile.reactive.messaging;


import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to signify a subscriber to incoming messages.
 * <p>
 * Methods annotated with this annotation must have one of the following shapes:
 * </p>
 * <ul>
 * <li>Take zero parameters, and return a {@link SubscriberBuilder} or a {@link org.reactivestreams.Subscriber}.</li>
 * <li>Take zero parameters, and return a {@link ProcessorBuilder} or a {@link org.reactivestreams.Processor}.</li>
 * <li>Accept a single parameter, and return a {@link java.util.concurrent.CompletionStage}.</li>
 * <li>Accept a single parameter, and return {@code void}.</li>
 * <li>Accept a single parameter, and return <em>any</em> type.</li>
 * </ul>
 * <p>
 * In addition, implementations of this specification may allow returning additional types, such as implementation
 * specific types for representing Reactive Streams, however taking advantage of these features provided by
 * implementations will result in a non portable application.
 * </p>
 * <p>
 * The type of the message accepted by the subscriber may be wrapped in {@link Message}, or a subclass of it. All
 * messaging providers must support {@code Message}, but messaging providers may also provide subclasses of
 * {@code Message} in order to expose message transport specific features. Use of these sub classes will result in
 * a non portable application. If the chosen messaging provider does not support the selected message wrapper, a
 * deployment exception will be raised before the container is initialized.
 * </p>
 * <p>
 * If the incoming message is wrapped in a {@code Message} wrapper, then it is the responsibility of the subscriber to
 * acknowledge messages. This can either by done by invoking {@link Message#ack()} directly, or if using a method
 * shape that has an output value (such as the processor shapes, or methods that return a value), and if the output
 * value also is also wrapped in a {@code Message}, by passing the {@code ack} callback to the emitted {@code Message}
 * so that the container can ack it.
 * </p>
 * <p>
 * If the incoming message is not wrapped, then the container is responsible for automatically acknowledging messages.
 * When the ack is done depends on the shape of the method - for subscriber shapes, it may either be done before or after
 * passing a message to the subscriber (note that it doesn't matter which, since compliant Reactive Streams
 * implementations don't allow throwing exceptions directly from the subscriber). For processor shapes, it should be
 * when the processor emits an element. In this case, it is assumed, and the application must ensure, that there is
 * a 1:1 correlation of elements consumed and emitted by the processor, and that ordering is maintained. For shapes
 * that return a {@code CompletionStage}, it should be when that completion stage is redeemed. For methods that
 * accept a single parameter and then return void or a value, it should be done after the method is invoked
 * successfully.
 * </p>
 * <p>
 * If there is an output value, and it is wrapped, then it is the containers responsibility to invoke
 * {@link Message#ack()} on each message emitted, except if indicated otherwise with the {@link Acknowledgment}
 * annotation.
 * </p>
 * <p>
 * {@code Incoming} annotated methods may also have an {@link Outgoing} annotation, in which case, they must have a
 * shape that emits an output value (such as a processor or a return value).
 * </p>
 * <p>
 * If the method has an output value but no {@link Outgoing} annotation, then the actual unwrapped value is ignored,
 * though wrapped messages must still have their {@code ack} callback invoked by the container.
 * </p>
 *
 * @see org.eclipse.microprofile.reactive.messaging
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Incoming {

    /**
     * The name of the consumed channel.
     *
     * @return the name of the consumed channel, must not be blank.
     */
    String value();
}
