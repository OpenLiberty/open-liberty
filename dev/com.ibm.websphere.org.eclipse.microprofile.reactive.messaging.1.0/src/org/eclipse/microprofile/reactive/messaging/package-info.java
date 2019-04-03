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
/**
 * The MicroProfile Reactive Messaging API.
 * <p>
 * This API provides a mechanism for declaring managed streams. CDI managed beans may declare methods annotated with
 * {@link org.eclipse.microprofile.reactive.messaging.Incoming} and/or
 * {@link org.eclipse.microprofile.reactive.messaging.Outgoing} to declare a message subscriber, publisher or processor.
 * <p>
 * The container is responsible for running, maintaining liveness, and stopping the message streams on context shutdown.
 * Containers should implement restarting in case a stream fails, with an appropriate backoff strategy in the event of
 * repeat failures.
 * <p>
 * The application should use Reactive Streams to provide the message stream handlers. Generally, use of
 * {@link org.eclipse.microprofile.reactive.streams} builders should be used in preference to either Reactive Streams
 * interfaces directly, or container specific implementations of streams.
 * <p>
 * Here is an example use of this API:
 * <pre>
 *   &#64;ApplicationScoped
 *   public class EmailPublisher {
 *     &#64;Incoming("notifications")
 *     &#64;Outgoing("emails")
 *     public ProcessorBuilder&lt;Message&lt;Notification&gt;, Message&lt;Email&gt;&gt; publishEmails() {
 *       return ReactiveStreams.&lt;Message&lt;Notification&gt;&gt;builder()
 *         .filter(msg -&gt; msg.getPayload().isEmailable())
 *         .map(msg -&gt; {
 *           Email email = convertNotificationToEmail(msg.getPayload());
 *           return Message.of(email, msg::ack);
 *         });
 *     }
 *
 *     private Email convertNotificationToEmail(Notification notification) {
 *       ...
 *     }
 *   }
 * </pre>
 */
package org.eclipse.microprofile.reactive.messaging;
