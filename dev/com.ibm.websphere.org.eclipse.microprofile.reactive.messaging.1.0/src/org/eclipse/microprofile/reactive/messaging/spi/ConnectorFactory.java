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
package org.eclipse.microprofile.reactive.messaging.spi;

/**
 * Parent class for {@link IncomingConnectorFactory} and {@link OutgoingConnectorFactory}.
 */
public interface ConnectorFactory {

    /**
     * The {@code channel-name} attribute name.
     *
     * This attribute is injected by the reactive messaging implementation into the
     * {@link org.eclipse.microprofile.config.Config} object passed to the {@link IncomingConnectorFactory} and
     * {@link OutgoingConnectorFactory}. The value associated with this attribute is the name of the channel being
     * created.
     */
    String CHANNEL_NAME_ATTRIBUTE = "channel-name";

    /**
     * The {@code connector} attribute name.
     *
     * This attribute is part of the {@link org.eclipse.microprofile.config.Config} passed to the
     * {@link IncomingConnectorFactory} and {@link OutgoingConnectorFactory} when a new channel is created. It
     * indicates the name of the connector.
     *
     * Note that each channel configured from the MicroProfile Config support must provide this attribute to indicate
     * which connector is used. It must match the name given to the {@link Connector} qualifier.
     */
    String CONNECTOR_ATTRIBUTE = "connector";

    /**
     * Prefix used in the MicroProfile Config to configure an {@code incoming} channel.
     */
    String INCOMING_PREFIX = "mp.messaging.incoming.";

    /**
     * Prefix used in the MicroProfile Config to configure an {@code outgoing} channel.
     */
    String OUTGOING_PREFIX = "mp.messaging.outgoing.";

    /**
     * Prefix used in the MicroProfile Config to configure properties shared by all the channels associated with a
     * specific connector.
     */
    String CONNECTOR_PREFIX = "mp.messaging.connector.";
}
