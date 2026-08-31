/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;

/**
 * Writes a JSON {@link JsonObject object} or {@link JsonArray array} structure
 * to an output source.
 *
 * <p>The class {@link jakarta.json.Json} contains methods to create writers from
 * output sources ({@link java.io.OutputStream} and {@link java.io.Writer}).
 *
 * <p>
 * The following example demonstrates how write an empty JSON object:
 * <pre>
 * <code>
 * JsonWriter jsonWriter = Json.createWriter(...);
 * jsonWriter.writeObject(Json.createObjectBuilder().build());
 * jsonWriter.close();
 * </code>
 * </pre>
 *
 * <p>
 * The class {@link JsonWriterFactory} also contains methods to create
 * {@code JsonWriter} instances. A factory instance can be used to create
 * multiple writer instances with the same configuration. This the preferred
 * way to create multiple instances. A sample usage is shown in the following
 * example:
 * <pre>
 * <code>
 * JsonWriterFactory factory = Json.createWriterFactory(config);
 * JsonWriter writer1 = factory.createWriter(...);
 * JsonWriter writer2 = factory.createWriter(...);
 * </code>
 * </pre>
 */
public interface JsonWriter extends  /*Auto*/Closeable {

    /**
     * Writes the specified JSON {@link JsonArray array} to the output
     * source. This method needs to be called only once for a writer instance.
     *
     * @param array JSON array that is to be written to the output source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write or close
     *     method is already called
     */
    void writeArray(JsonArray array);

    /**
     * Writes the specified JSON {@link JsonObject object} to the output
     * source. This method needs to be called only once for a writer instance.
     *
     * @param object JSON object that is to be written to the output source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write or close
     *     method is already called
     */
    void writeObject(JsonObject object);

    /**
     * Writes the specified JSON {@link JsonObject object} or
     * {@link JsonArray array} to the output source. This method needs
     * to be called only once for a writer instance.
     *
     * @param value JSON array or object that is to be written to the output
     *              source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write
     *     or close method is already called
     */
    void write(JsonStructure value);

    /**
     * Closes this JSON writer and frees any resources associated with the
     * writer. This method closes the underlying output source.
     *
     * @throws JsonException if an i/o error occurs (IOException would be
     * cause of JsonException)
     */

    /**
     * Writes the specified {@link JsonValue} to the output source.
     * method needs to be called only once for a write instance.
     *
     * @param value a {@code JsonValue} to be written to the output
     *              source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write
     *     or close method is already called
     *
     * @since 1.1
     */
    default void write(JsonValue value) {
        throw new UnsupportedOperationException();
    }

    @Override
    void close();

}
