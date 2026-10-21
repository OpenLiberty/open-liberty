/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

import java.util.Collection;
import java.util.Map;

/**
 * Factory to create {@link JsonObjectBuilder} and {@link JsonArrayBuilder}
 * instances. If a factory instance is configured with some configuration,
 * that would be used to configure the created builder instances.
 *
 * <p>
 * {@code JsonObjectBuilder} and {@code JsonArrayBuilder} can also be created
 * using {@link Json}'s methods. If multiple builder instances are created,
 * then creating them using a builder factory is preferred.
 *
 * <p>
 * <b>For example:</b>
 * <pre>
 * <code>
 * JsonBuilderFactory factory = Json.createBuilderFactory(...);
 * JsonArray value = factory.createArrayBuilder()
 *     .add(factory.createObjectBuilder()
 *         .add("type", "home")
 *         .add("number", "212 555-1234"))
 *     .add(factory.createObjectBuilder()
 *         .add("type", "fax")
 *         .add("number", "646 555-4567"))
 *     .build();
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonBuilderFactory {

    /**
     * Creates a {@code JsonObjectBuilder} instance that is used to build
     * {@link JsonObject}.
     *
     * @return a JSON object builder
     */
    JsonObjectBuilder createObjectBuilder();

    /**
     * Creates a {@code JsonObjectBuilder} instance, initialized with an object.
     *
     * @param object the initial object in the builder
     * @return a JSON object builder
     * @throws NullPointerException if specified object is {@code null}
     *
     * @since 1.1
     */
    default JsonObjectBuilder createObjectBuilder(JsonObject object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonObjectBuilder} instance, initialized with the specified object.
     *
     * @param object the initial object in the builder
     * @return a JSON object builder
     * @throws NullPointerException if specified object is {@code null}
     *
     * @since 1.1
     */
    default JsonObjectBuilder createObjectBuilder(Map<String, Object> object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonArrayBuilder} instance that is used to build
     * {@link JsonArray}
     *
     * @return a JSON array builder
     */
    JsonArrayBuilder createArrayBuilder();

    /**
     * Creates a {@code JsonArrayBuilder} instance, initialized with an array.
     *
     * @param array the initial array in the builder
     * @return a JSON array builder
     * @throws NullPointerException if specified array is {@code null}
     *
     * @since 1.1
     */
    default JsonArrayBuilder createArrayBuilder(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonArrayBuilder} instance,
     * initialized with the content of specified collection.
     *
     * @param collection the initial data for the builder
     * @return a JSON array builder
     * @throws NullPointerException if specified collection is {@code null}
     *
     * @since 1.1
     */
    default JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns read-only map of supported provider specific configuration
     * properties that are used to configure the created JSON builders.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the builders. The map be empty but not null.
     */
    Map<String, ?> getConfigInUse();

}
