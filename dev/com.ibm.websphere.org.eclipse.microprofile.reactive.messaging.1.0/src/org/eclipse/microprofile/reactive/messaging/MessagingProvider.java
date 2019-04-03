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

/**
 * Marker interface to indicate which messaging provider should be used.
 * <p>
 * If a container supports more than one messaging provider, then they may use this to differentiate between messaging
 * providers by providing a subclass for each provider. Application streams can then use this subclass by passing it
 * to the {@code provider} property of their {@link Incoming} or {@link Outgoing} annotations.
 * <p>
 * Unless containers explicitly share libraries that provide the same messaging provider using the same sub class of
 * this type, then messaging providers are assumed to be specific to each container. That is to say, if one container
 * supports a messaging technology called Reactive Messaging, and provides a subclass of this called
 * {@code org.example.container.ReactiveMessagingProvider}, and another container also supports the same technology,
 * it won't necessarily use the same class, and from the containers point of view, the two different classes
 * representing the same technology are for two different technologies.
 *
 * @see Incoming#provider()
 * @see Outgoing#provider()
 */
public interface MessagingProvider {
}
