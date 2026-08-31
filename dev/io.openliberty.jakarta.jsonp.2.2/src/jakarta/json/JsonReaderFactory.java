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

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory to create {@link jakarta.json.JsonReader} instances. If a factory
 * instance is configured with some configuration, that would be
 * used to configure the created reader instances.
 *
 * <p>
 * {@link jakarta.json.JsonReader} can also be created using {@link Json}'s
 * {@code createReader} methods. If multiple reader instances are created,
 * then creating them using a reader factory is preferred.
 *
 * <p>
 * <b>For example:</b>
 * <pre>
 * <code>
 * JsonReaderFactory factory = Json.createReaderFactory(...);
 * JsonReader reader1 = factory.createReader(...);
 * JsonReader reader2 = factory.createReader(...);
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonReaderFactory {

    /**
     * Creates a JSON reader from a character stream. The reader is configured
     * with the factory configuration.
     *
     * @param reader a reader from which JSON is to be read
     * @return a JSON reader
     */
    JsonReader createReader(Reader reader);

    /**
     * Creates a JSON reader from a byte stream. The character encoding of
     * the stream is determined as described in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     * The reader is configured with the factory configuration.
     *
     * @param in a byte stream from which JSON is to be read
     * @return a JSON reader
     */
    JsonReader createReader(InputStream in);

    /**
     * Creates a JSON reader from a byte stream. The bytes of the stream
     * are decoded to characters using the specified charset. The reader is
     * configured with the factory configuration.
     *
     * @param in a byte stream from which JSON is to be read
     * @param charset a charset
     * @return a JSON reader
     */
    JsonReader createReader(InputStream in, Charset charset);

    /**
     * Returns read-only map of supported provider specific configuration
     * properties that are used to configure the created JSON readers.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the readers. The map be empty but not null.
     */
    Map<String, ?> getConfigInUse();

}
