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
 * The base interface for OpenAPI model objects that can contain extensions. Extensions contain data not required by the specification and may or may
 * not be supported by the tools you use.
 * <p>
 * The extensions property names are always prefixed by "x-".
 */
public interface Extensible {

    /**
     * Returns the extensions property from an Extensible instance.
     *
     * @return a map containing keys which start with "x-" and values which provide additional information
     **/
    Map<String, Object> getExtensions();

    /**
     * Adds the given object to this Extensible's map of extensions, with the given name as its key.
     *
     * @param name the key used to access the extension object. Always prefixed by "x-".
     * @param value data not required by the specification
     */
    void addExtension(String name, Object value);

    /**
     * Sets this Extensible's extensions property to the given map of extensions.
     *
     * @param extensions map containing keys which start with "x-" and values which provide additional information
     */
    void setExtensions(Map<String, Object> extensions);

}
