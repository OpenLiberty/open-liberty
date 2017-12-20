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

package org.eclipse.microprofile.openapi.models;

/**
 * ExternalDocumentation
 * <p>
 * Allows referencing an external resource for extended documentation.
 * <p>
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#externalDocumentationObject">OpenAPI Specification
 *      External Documentation Object</a>
 */
public interface ExternalDocumentation extends Constructible, Extensible {

    /**
     * Returns the description property from an ExternalDocumentation instance.
     *
     * @return a short description of the target documentation
     **/
    String getDescription();

    /**
     * Sets this ExternalDocumentation's description property to the given string.
     *
     * @param description a short description of the target documentation
     */
    void setDescription(String description);

    /**
     * Sets this ExternalDocumentation's description property to the given string.
     *
     * @param description a short description of the target documentation
     * @return the current ExternalDocumentation instance
     */
    ExternalDocumentation description(String description);

    /**
     * Returns the url property from an ExternalDocumentation instance.
     *
     * @return the URL for the target documentation
     **/
    String getUrl();

    /**
     * Sets this ExternalDocumentation's url property to the given string.
     *
     * @param url the URL for the target documentation
     */
    void setUrl(String url);

    /**
     * Sets this ExternalDocumentation's url property to the given string.
     *
     * @param url the URL for the target documentation
     * @return the current ExternalDocumentation instance
     */
    ExternalDocumentation url(String url);

}