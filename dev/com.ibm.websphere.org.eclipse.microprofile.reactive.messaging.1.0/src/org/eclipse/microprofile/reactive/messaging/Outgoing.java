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
 * <li>Take zero parameters, and return a {@link org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder}.
 * </li>
 * <li>Take zero parameters, and return a {@link org.reactivestreams.Publisher}.</li>
 * <li>Take zero parameters, and return a {@link org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder}.
 * </li>
 * <li>Take zero parameters, and return a {@link org.reactivestreams.Processor}.</li>
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
     * The name of the stream to publish to.
     * <p>
     * If not set, it is assumed some other messaging provider specific mechanism will be used to identify the
     * destination that this publisher will send to.
     * </p>
     *
     * @return the name of the stream
     */
    String value() default "";

    /**
     * The messaging provider.
     * <p>
     * If not set, then the container may provide a container specific mechanism for selecting a default messaging
     * provider.
     * </p>
     * <p>
     * Note that not all messaging providers are compatible with all containers, it is up to each container to
     * decide which messaging providers it will accept, to define the messaging provider classes to pass here, and
     * to potentially offer a container specific extension point for plugging in new containers.
     * </p>
     * <p>
     * If the container does not support the selected messaging provider, it must raise a deployment exception before
     * the container is initialized.
     * </p>
     * <p>
     * The use of this property is inherently non portable.
     * </p>
     *
     * @return the messaging provider
     */
    Class<? extends MessagingProvider> provider() default MessagingProvider.class;
}
