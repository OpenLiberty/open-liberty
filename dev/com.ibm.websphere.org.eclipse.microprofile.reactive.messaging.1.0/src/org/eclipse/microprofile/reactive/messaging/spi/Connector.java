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


import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier used on connector implementations to indicate the associated underlying transport.
 * <p>
 * The value indicates the name associated with  the bean implementing either
 * {@link IncomingConnectorFactory} or {@link OutgoingConnectorFactory} or both.
 * <p>
 * Note that the given name is a user-facing interface used in the configuration.
 */
@Qualifier
@Retention(RUNTIME)
@Target(TYPE)
public @interface Connector {

    /**
     * @return the name of the connector associated with the bean implementing {@link IncomingConnectorFactory}
     * or {@link OutgoingConnectorFactory}. Must not be {@code null}. Returning {@code null} will cause a deployment
     * failure.
     */
    String value();

}
