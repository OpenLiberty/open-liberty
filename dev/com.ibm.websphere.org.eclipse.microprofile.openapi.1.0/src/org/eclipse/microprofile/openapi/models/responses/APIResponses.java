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

/**
 * This interface represents the container for the expected responses of an operation. The container maps a HTTP response code to the expected
 * response.
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#responses-object">Responses Object</a>
 */
public interface APIResponses extends Constructible, Map<String, APIResponse> {

    public static final String DEFAULT = "default";

    /**
     * Adds an ApiResponse in the format of the name as a key and the item as the value to ApiResponses map
     * 
     * @param name the name of ApiResponse 
     * @param apiResponse the ApiResponse object to be added to ApiResponses map
     * @return ApiResponses map with the added ApiResponse instance
     **/
    APIResponses addApiResponse(String name, APIResponse apiResponse);

    /**
     * Returns the default documentation of responses other than the ones declared for specific HTTP response codes in this instance of ApiResponses.
     *
     * @return the default documentation of responses
     **/

    APIResponse getDefault();

    /**
     * Sets the default documentation of responses for this instance of ApiResponses. This will cover all the undeclared responses.
     *
     * @param defaultValue the default documentation of responses
     */

    void setDefaultValue(APIResponse defaultValue);

    /**
     * Sets the default documentation of responses for this instance of ApiResponses and return this instance of ApiResponses. This will cover all the
     * undeclared responses.
     *
     * @param defaultValue the default documentation of responses
     * @return this ApiResponses instance
     */

    APIResponses defaultValue(APIResponse defaultValue);

}