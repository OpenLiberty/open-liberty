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

import javax.enterprise.util.AnnotationLiteral;

/**
 * Supports inline instantiation of the {@link Connector} qualifier.
 */
public final class ConnectorLiteral extends AnnotationLiteral<Connector> implements Connector {

    private static final long serialVersionUID = 1L;

    private final String value;

    /**
     * Creates a new instance of {@link ConnectorLiteral}.
     *
     * @param value the name of the connector, must not be {@code null}, must not be {@code blank}
     * @return the {@link ConnectorLiteral} instance.
     */
    public static Connector of(String value) {
        return new ConnectorLiteral(value);
    }

    /**
     * Creates a new instance of {@link ConnectorLiteral}.
     * Users should use the {@link #of(String)} method to create instances.
     *
     * @param value the value.
     */
    private ConnectorLiteral(String value) {
        this.value = value;
    }

    /**
     * @return the connector name.
     */
    public String value() {
        return value;
    }
}

