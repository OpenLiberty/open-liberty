/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.models.media;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.examples.Example;

/**
 * MediaType
 * <p>
 * Each Media Type Object provides a schema and examples for the media type identified by its key.
 * <p>
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#mediaTypeObject">OpenAPI Specification Media Type
 *      Object</a>
 */
@SuppressWarnings("rawtypes")
public interface MediaType extends Constructible, Extensible {

    /**
     * Returns the schema property from a MediaType instance.
     *
     * @return the schema defining the type used for the request body
     **/
    Schema getSchema();

    /**
     * Sets the schema field of a MediaType instance to the given schema object.
     *
     * @param schema the schema defining the type used for the request body
     */
    void setSchema(Schema schema);

    /**
     * Sets the schema field of a MediaType instance to the given schema object.
     *
     * @param schema the schema defining the type used for the request body
     * @return the current MediaType instance
     */
    MediaType schema(Schema schema);

    /**
     * Returns the collection of examples from a MediaType instance.
     *
     * @return examples of the media type
     **/
    Map<String, Example> getExamples();

    /**
     * Sets the examples field of a MediaType instance to the given map object. Each example object should match the media type and specified schema
     * if present. The example object is mutually exclusive of the examples object.
     *
     * @param examples examples of the media type
     */
    void setExamples(Map<String, Example> examples);

    /**
     * Sets the examples field of a MediaType instance to the given map object. Each example object should match the media type and specified schema
     * if present. The example object is mutually exclusive of the examples object.
     *
     * @param examples examples of the media type
     * @return the current MediaType instance
     */
    MediaType examples(Map<String, Example> examples);

    /**
     * Adds an example item to the examples map of a MediaType instance. The example object should match the media type and specified schema if
     * present.
     *
     * @param key any unique name to identify the example object
     * @param example an example of a media type
     * @return the current MediaType instance
     */
    MediaType addExample(String key, Example example);

    /**
     * Returns the example property from a MediaType instance.
     *
     * @return an example of the media type
     **/
    Object getExample();

    /**
     * Sets the example property of a MediaType instance to the given value. The example object should be in the correct format as specified by the
     * media type. The example object is mutually exclusive of the examples object.
     *
     * @param example an example of the media type
     */
    void setExample(Object example);

    /**
     * Sets the example property of a MediaType instance to the given value. The example object should be in the correct format as specified by the
     * media type. The example object is mutually exclusive of the examples object.
     *
     * @param example an example of the media type
     * @return the current MediaType instance
     */
    MediaType example(Object example);

    /**
     * Returns the encoding property from a MediaType instance.
     *
     * @return a map between a property name and its encoding information
     **/
    Map<String, Encoding> getEncoding();

    /**
     * Sets encoding property of a MediaType instance to the given map object.
     *
     * @param encoding a map between property names and their encoding information
     */
    void setEncoding(Map<String, Encoding> encoding);

    /**
     * Sets encoding property of a MediaType instance to the given map object.
     *
     * @param encoding a map between property names and their encoding information
     * @return the current MediaType instance
     */
    MediaType encoding(Map<String, Encoding> encoding);

    /**
     * Adds an Encoding item to the encoding property of a MediaType instance.
     *
     * @param key a property name in the schema
     * @param encodingItem an encoding definition to apply to the schema property.
     * @return the current MediaType instance
     */
    MediaType addEncoding(String key, Encoding encodingItem);

}