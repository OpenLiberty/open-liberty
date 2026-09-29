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

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory to create {@link jakarta.json.JsonWriter} instances. If a factory
 * instance is configured with some configuration, that would be
 * used to configure the created writer instances.
 *
 * <p>
 * {@link jakarta.json.JsonWriter} can also be created using {@link Json}'s
 * {@code createWriter} methods. If multiple writer instances are created,
 * then creating them using a writer factory is preferred.
 *
 * <p>
 * <b>For example:</b>
 * <pre>
 * <code>
 * JsonWriterFactory factory = Json.createWriterFactory(...);
 * JsonWriter writer1 = factory.createWriter(...);
 * JsonWriter writer2 = factory.createWriter(...);
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonWriterFactory {

    /**
     * Creates a JSON writer to write a JSON {@link JsonObject object} or
     * {@link JsonArray array} structure to the specified character stream.
     * The writer is configured with the factory configuration.
     *
     * @param writer to which JSON object or array is written
     * @return a JSON writer
     */
    JsonWriter createWriter(Writer writer);

    /**
     * Creates a JSON writer to write a JSON {@link JsonObject object} or
     * {@link JsonArray array} structure to the specified byte stream.
     * Characters written to the stream are encoded into bytes using UTF-8
     * encoding. The writer is configured with the factory configuration.
     *
     * @param out to which JSON object or array is written
     * @return a JSON writer
     */
    JsonWriter createWriter(OutputStream out);

    /**
     * Creates a JSON writer to write a JSON {@link JsonObject object} or
     * {@link JsonArray array} structure to the specified byte stream.
     * Characters written to the stream are encoded into bytes using the
     * specified charset. The writer is configured with the factory
     * configuration.
     *
     * @param out to which JSON object or array is written
     * @param charset a charset
     * @return a JSON writer
     */
    JsonWriter createWriter(OutputStream out, Charset charset);

    /**
     * Returns read-only map of supported provider specific configuration
     * properties that are used to configure the created JSON writer objects.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the created writers. The map may be empty but not null.
     */
    Map<String, ?> getConfigInUse();

}
