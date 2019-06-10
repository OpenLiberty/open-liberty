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
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to signify a publisher of outgoing messages.
 * <p>
 * Methods annotated with this annotation must have one of the following shapes:
 * </p>
 * <ul>
 * <li>Take zero parameters, and return a {@link PublisherBuilder}, or a {@link org.reactivestreams.Publisher}.</li>
 * <li>Take zero parameters, and return a {@link ProcessorBuilder} or a {@link org.reactivestreams.Processor}.</li>
 * <li>Accept a single parameter, and return a {@link java.util.concurrent.CompletionStage}.</li>
 * <li>Accept a single parameter, and return any type.</li>
 * </ul>
 * <p>
 * In addition, implementations of this specification may allow returning additional types, such as implementation
 * specific types for representing Reactive Streams, however taking advantage of these features provided by
 * implementations will result in a non portable application.
 * </p>
 * <p>
 * The method shapes that return a processor, or accept a single parameter, must also have an {@link Incoming}
 * annotation, methods that do not have this will cause a definition exception to be raised by the container during
 * initialization.
 * </p>
 * <p>
 * The type of the message emitted by the publisher may be wrapped in {@link Message}, or a subclass of it. All
 * messaging providers must support {@code Message}, but messaging providers may also provide subclasses of
 * {@code Message} in order to expose message transport specific features. Use of these sub classes will result in
 * a non portable application. If the chosen messaging provider does not support the selected message wrapper, a
 * deployment exception will be raised before the container is initialized.
 * </p>
 * <p>
 * If the outing message is wrapped in a {@code Message} wrapper, then it is the responsibility of the container to
 * acknowledge messages, by invoking the {@link Message#ack()} method on each message it receives. Containers must be
 * careful to invoke these messages in order, one at a time, unless configured not to through a container specific
 * mechanism. Containers may provide the ability to configure batching of acks, or only acking one in every n
 * messages.
 * </p>
 *
 * @see org.eclipse.microprofile.reactive.messaging
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outgoing {

    /**
     * The name of the channel to publish to.
     *
     * @return the name of the channel, must not be blank.
     */
    String value();
}
