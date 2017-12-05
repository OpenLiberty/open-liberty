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
 * Base interface for OpenAPI model objects that can be references to other objects.
 */
public interface Reference<T extends Reference<T>> {

    /**
     * Returns the reference property from this Reference instance.
     *
     * @return a reference to a T object in the components in this OpenAPI document
     **/
    String getRef();

    /**
     * Sets this Reference's reference property to the given string.
     *
     * @param ref a reference to a T object in the components in this OpenAPI document
     **/
    void setRef(String ref);

    /**
     * Sets this Reference's reference property to the given string.
     *
     * @param ref a reference to a T object in the components in this OpenAPI document
     * @return the current instance
     **/
    T ref(String ref);

}
