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

/**
 * Discriminator
 * <p>
 * When request bodies or response payloads may be one of a number of different schemas, a discriminator object can be used to aid in serialization,
 * deserialization, and validation. The discriminator is a specific object in a schema which is used to inform the consumer of the specification of an
 * alternative schema based on the value associated with it.
 * <p>
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#discriminator-object">OpenAPI Specification Discriminator
 *      Object</a>
 */
public interface Discriminator extends Constructible {

    /**
     * Sets this Discriminator's propertyName property to the given string.
     *
     * @param propertyName the name of the property in the payload that will hold the discriminator value
     * @return the current Discriminator instance
     */
    Discriminator propertyName(String propertyName);

    /**
     * Returns the propertyName property from a Discriminator instance.
     *
     * @return the name of the property in the payload that will hold the discriminator value
     **/
    String getPropertyName();

    /**
     * Sets this Discriminator's propertyName property to the given propertyName.
     *
     * @param propertyName the name of the property in the payload that will hold the discriminator value
     */
    void setPropertyName(String propertyName);

    /**
     * Maps the given name to the given value and stores it in this Discriminator's mapping property.
     * 
     * @param name a key which will be compared to information from a request body or response payload.
     * @param value a schema name or reference
     * @return the current Discriminator instance
     */
    Discriminator addMapping(String name, String value);

    /**
     * Sets this Discriminator's mapping property to the given map object.
     *
     * @param mapping a map containing keys and schema names or references
     * @return the current Discriminator instance
     */
    Discriminator mapping(Map<String, String> mapping);

    /**
     * Returns the mapping property from a Discriminator instance.
     *
     * @return a map containing keys and schema names or references
     **/
    Map<String, String> getMapping();

    /**
     * Sets this Discriminator's mapping property to the given map object.
     *
     * @param mapping a map containing keys and schema names or references
     */
    void setMapping(Map<String, String> mapping);

}