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

package org.eclipse.microprofile.openapi.models.responses;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;

/**
 * This interface represents a single response from an API Operation, including design-time, static links to operations based on the response.
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#responseObject"
 */
public interface APIResponse extends Constructible, Extensible, Reference<APIResponse> {

    /**
     * Returns a short description of this instance of ApiResponse.
     *
     * @return a short description of the response
     **/

    String getDescription();

    /**
     * Sets the description of this instance of ApiResponse.
     *
     * @param description a short description of the response
     */

    void setDescription(String description);

    /**
     * Sets the description of this instance of ApiResponse and returns this ApiResponse instance.
     *
     * @param description a short description of the response
     * @return this ApiResponse instance
     */

    APIResponse description(String description);

    /**
     * Returns the map of Headers in this instance of ApiResponse.
     *
     * @return the headers of this response
     **/

    Map<String, Header> getHeaders();

    /**
     * Sets the Headers for this instance of ApiResponse with the given map of Headers. The Header names are case insensitive and if a Header is
     * defined with the name 'Content-Type', then it will be ignored.
     *
     * @param headers the headers of the response
     */

    void setHeaders(Map<String, Header> headers);

    /**
     * Sets the Headers for this instance of ApiResponse with the given map of Headers and returns this instance of ApiResponse. The Header names are
     * case insensitive and if a Header is defined with the name 'Content-Type', then it will be ignored.
     *
     * @param headers the headers of the response
     * @return this ApiResponse instance
     */

    APIResponse headers(Map<String, Header> headers);

    /**
     * Adds the given Header to this ApiResponse instance's map of Headers with the given name and return this instance of ApiResponse. If this
     * ApiResponse instance does not have any headers, a new map is created and the given header is added.
     *
     * @param name the unique name of the header
     * @param header a header for the response
     * @return this ApiResponse instance
     */

    APIResponse addHeader(String name, Header header);

    /**
     * Returns the map containing descriptions of potential response payload for this instance of ApiResponse.
     *
     * @return the potential content of the response
     **/

    Content getContent();

    /**
     * Sets the map containing descriptions of potential response payload for this instance of ApiResponse.
     *
     * @param content the potential content of the response
     */

    void setContent(Content content);

    /**
     * Sets the map containing descriptions of potential response payload for this instance of ApiResponse and returns this ApiResponse instance.
     *
     * @param content the potential content of the response
     * @return this ApiResponse instance
     */

    APIResponse content(Content content);

    /**
     * Returns the operations links that can be followed from tis instance of ApiResponse.
     *
     * @return operation links that can be followed from the response
     **/

    Map<String, Link> getLinks();

    /**
     * Sets the operations links that can be followed from tis instance of ApiResponse.
     *
     * @param links the operation links followed from the response
     */

    void setLinks(Map<String, Link> links);

    /**
     * Sets the operations links for this instance of ApiResponse using the given name and Link, and returns this ApiResponse instance.
     *
     * @param name the short name of the link
     * @param link the operation link that can be followed from the response
     * @return this ApiResponse instance
     */

    APIResponse link(String name, Link link);

}