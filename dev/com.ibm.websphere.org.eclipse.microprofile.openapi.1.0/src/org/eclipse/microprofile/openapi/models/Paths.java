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

import java.util.Map;

/**
 * Paths
 * <p>
 * Holds the relative paths to the individual endpoints and their operations. The path is appended to the URL from the Server Object in order to
 * construct the full URL. The Paths MAY be empty, due to
 * <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#security-filtering">security constraints</a>.
 * <p>
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#pathsObject"> OpenAPI Specification Paths Object</a>
 */
public interface Paths extends Constructible, Extensible, Map<String, PathItem> {

    /**
     * Adds the given path item to this Paths and return this instance of Paths
     * 
     * @param name a path name in the format valid for a Paths object. The field name MUST begin with a slash.
     * @param item the path item added to the list of paths
     * @return the current Paths instance
     */
    Paths addPathItem(String name, PathItem item);

}