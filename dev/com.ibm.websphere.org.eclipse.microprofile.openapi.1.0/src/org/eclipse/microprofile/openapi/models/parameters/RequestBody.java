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

package org.eclipse.microprofile.openapi.models.parameters;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.Content;

/**
 * This interface represents the request body of an operation in which body parameters can be specified.
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#requestBodyObject">requestBody Object</a>
 */
public interface RequestBody extends Constructible, Extensible, Reference<RequestBody> {

    /**
     * Returns the description of this instance of RequestBody.
     *
     * @return a brief description of the RequestBody
     **/

    String getDescription();

    /**
     * Sets the description of this instance of RequestBody. to the parameter.
     *
     * @param description the brief description of the RequestBody
     */

    void setDescription(String description);

    /**
     * Sets the description of this RequestBody and return this instance of RequestBody
     *
     * @param description the brief description of the RequestBody
     * @return this RequestBody instance
     */

    RequestBody description(String description);

    /**
     * Returns the content of this instance of RequestBody, where the keys in content are media type names and the values describe it.
     *
     * @return the content of this RequestBody
     **/

    Content getContent();

    /**
     * Sets the content of this instance of RequestBody, where the keys in content are media type names and the values describe it.
     *
     * @param content the content that describes the RequestBody
     */

    void setContent(Content content);

    /**
     * Sets the content of this instance of RequestBody, where the keys in content are media type names and the values describe it.
     *
     * @param content the content that describes the RequestBody
     * @return RequestBody instance with the modified content property
     */

    RequestBody content(Content content);

    /**
     * Returns whether this instance of RequestBody is required for the operation.
     *
     * @return true iff the RequestBody is required, false otherwise
     **/

    Boolean getRequired();

    /**
     * Sets whether this instance of RequestBody is required or not.
     *
     * @param required true iff the RequestBody is required, false otherwise
     */

    void setRequired(Boolean required);

    /**
     * Sets whether this instance of RequestBody is required or not and returns this instance of RequestBody
     *
     * @param required true iff the RequestBody is required, false otherwise
     * @return this RequestBody instance
     */

    RequestBody required(Boolean required);

}