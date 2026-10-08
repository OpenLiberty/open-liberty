/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
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

package jakarta.json.stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory for creating {@link JsonParser} instances. If a factory
 * instance is configured with a configuration, the configuration applies
 * to all parser instances created using that factory instance.
 *
 * <p>
 * The class {@link jakarta.json.Json Json} also provides methods to create
 * {@link JsonParser} instances, but using {@code JsonParserFactory} is 
 * preferred when creating multiple parser instances as shown in the following
 * example:
 *
 * <pre>
 * <code>
 * JsonParserFactory factory = Json.createParserFactory();
 * JsonParser parser1 = factory.createParser(...);
 * JsonParser parser2 = factory.createParser(...);
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonParserFactory {

    /**
     * Creates a JSON parser from a character stream.
     *
     * @param reader a i/o reader from which JSON is to be read
     * @return the created JSON parser
     */
    JsonParser createParser(Reader reader);

    /**
     * Creates a JSON parser from the specified byte stream.
     * The character encoding of the stream is determined
     * as specified in <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in i/o stream from which JSON is to be read
     * @return the created JSON parser
     * @throws jakarta.json.JsonException if encoding cannot be determined
     *         or i/o error (IOException would be cause of JsonException)
     */
    JsonParser createParser(InputStream in);

    /**
     * Creates a JSON parser from the specified byte stream.
     * The bytes of the stream are decoded to characters using the
     * specified charset.
     *
     * @param in i/o stream from which JSON is to be read
     * @param charset a charset
     * @return the created JSON parser
     */
    JsonParser createParser(InputStream in, Charset charset);

    /**
     * Creates a JSON parser from the specified JSON object.
     *
     * @param obj a JSON object
     * @return the created JSON parser
     */
    JsonParser createParser(JsonObject obj);

    /**
     * Creates a JSON parser from the specified JSON array.
     *
     * @param array a JSON array
     * @return the created JSON parser
     */
    JsonParser createParser(JsonArray array);

    /**
     * Returns a read-only map of supported provider specific configuration
     * properties that are used to configure the JSON parsers.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the created parsers. The map may be empty but not null
     */
    Map<String, ?> getConfigInUse();

}
