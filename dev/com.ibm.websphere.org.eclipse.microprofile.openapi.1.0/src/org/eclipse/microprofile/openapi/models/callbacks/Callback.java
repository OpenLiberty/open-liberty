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

package org.eclipse.microprofile.openapi.models.callbacks;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;

/**
 * Callback
 * <p>
 * A map of possible out-of-band callbacks related to the parent operation. Each value in the map is a Path Item Object that describes a set of
 * requests that may be initiated by the API provider and the expected responses. The key value used to identify the callback object is an expression,
 * evaluated at runtime, that identifies a URL to use for the callback operation.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#callbackObject">OpenAPI Specification Callback Object</a>
 */
public interface Callback extends Constructible, Extensible, Reference<Callback>, Map<String, PathItem> {

    /**
     * Adds the given PathItem to this Callback's list of PathItems using the string as its key.
     *
     * The key that identifies the Path Item Object is a runtime expression that can be evaluated in the context of a runtime HTTP request/response to
     * identify the URL to be used for the callback request. A simple example might be $request.body#/url. However, using a runtime expression the
     * complete HTTP message can be accessed. This includes accessing any part of a body that a JSON Pointer RFC6901 can reference.
     * 
     * @param name a runtime expression that can be evaluated in the context of a runtime HTTP request/response
     * @param pathItem a path to add to this Callback's list of PathItems
     * @return the current Callback instance
     */
    Callback addPathItem(String name, PathItem pathItem);

}