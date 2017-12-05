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

package org.eclipse.microprofile.openapi.models.info;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * This interface represents the License information for the exposed API.
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#licenseObject"
 */
public interface License extends Constructible, Extensible {

    /**
     * Returns the license name for this License instance that is used for the API.
     *
     * @return the license name used for the API
     **/
    String getName();

    /**
     * Sets the license name for this License instance that is used for the API.
     *
     * @param name the license name used for the API
     */
    void setName(String name);

    /**
     * Sets this License instance's name used for the API and returns this instance of License.
     *
     * @param name the license name used for the API
     * @return this License instance
     */
    License name(String name);

    /**
     * Returns the URL for this License instance that is used for the API.
     *
     * @return the URL to the license used for the API
     **/

    String getUrl();

    /**
     * Sets this URL for this License instance that is used for the API.
     *
     * @param url the URL to the license used for the API
     */
    void setUrl(String url);

    /**
     * Sets this License instance's URL used for the API and returns this instance of License.
     *
     * @param url the URL to the license used for the API
     * @return this License instance
     */
    License url(String url);

}