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
 * Content
 * <p>
 * A map to assist describing the media types for an operation's parameter or response.
 * 
 */
public interface Content extends Constructible, Map<String, MediaType> {

    /**
     * Adds the MediaType for this Content, where the key is the name of the MediaType and the value is the object that describes the content passed
     * into or returned from an operation.
     *
     * @param name the name of a media type e.g. application/json.
     * @param mediaType an object that describes the content passed into or returned from an operation.
     * @return the current Content instance
     */
    Content addMediaType(String name, MediaType mediaType);

}