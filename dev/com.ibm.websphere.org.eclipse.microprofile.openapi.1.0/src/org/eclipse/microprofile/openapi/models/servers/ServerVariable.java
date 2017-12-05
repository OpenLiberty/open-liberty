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

package org.eclipse.microprofile.openapi.models.servers;

import java.util.List;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * An object representing a Server Variable for server URL template substitution.
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#serverVariableObject">ServerVariable Object</a>
 */
public interface ServerVariable extends Constructible, Extensible {

    /**
     * This method returns the enumeration property of ServerVariable instance.
     * <p>
     * This property represents an enumeration of string values to be used if the substitution options are from a limited set
     * </p> 
     * @return List&lt;String&gt; enumeration
     **/
    List<String> getEnumeration();

    /**
     * This method sets the enumeration property of ServerVariable instance to the given enumeration argument.
     * <p>
     * This property represents an enumeration of string values to be used if the substitution options are from a limited set
     * </p>
     * @param enumeration an list of string values to be used if the substitution options are from a limited set
     */
    void setEnumeration(List<String> enumeration);

    /**
     * This method sets the enumeration property of ServerVariable instance to the given enumeration argument and returns the modified instance.
     * <p>
     * This property represents an enum of string values to be used if the substitution options are from a limited set.
     * </p>
     * @param enumeration an list of string values to be used if the substitution options are from a limited set
     * @return ServerVariable instance with the set enumeration property
     */
    ServerVariable enumeration(List<String> enumeration);

    /**
     * This method adds a string item to enumeration list of a ServerVariable instance and returns the instance.
     * <p>
     * If the enumeration list is null, this method should create a new ArrayList and add the item.
     * </p>
     * @param enumeration an item to be added to enum list
     * @return ServerVariable instance with the added enum item.
     */
    ServerVariable addEnumeration(String enumeration);

    /**
     * The default value to use for substitution, and to send, if an alternate value is not supplied. This value MUST be provided by the consumer and
     * is REQUIRED.
     * <p>
     * This method returns the defaultValue property from ServerVariable instance.
     * </p> 
     * @return String defaultValue
     **/
    String getDefaultValue();

    /**
     * The default value to use for substitution, and to send, if an alternate value is not supplied. This value MUST be provided by the consumer and
     * is REQUIRED.
     * <p>
     * This method sets the defaultValue property of ServerVariable instance to the given defaultValue argument.
     * </p>
     * @param defaultValue the value to use for substitution, and to send, if an alternate value is not supplied
     */
    void setDefaultValue(String defaultValue);

    /**
     * The default value to use for substitution, and to send, if an alternate value is not supplied. This value MUST be provided by the consumer and
     * is REQUIRED.
     * <p>
     * This method sets the defaultValue property of ServerVariable instance to the given defaultValue argument and returns the modified instance.
     * </p>
     * @param defaultValue the value to use for substitution, and to send, if an alternate value is not supplied
     * @return ServerVariable instance with the set defaultValue property
     */

    ServerVariable defaultValue(String defaultValue);

    /**
     * This method returns the description property of ServerVariable instance. Description property is optional for server variable.
     * @return String description
     **/
    String getDescription();

    /**
     * This method sets the description property of ServerVariable instance to the given description argument.
     * <p>
     * Description property is optional for server variable. CommonMark syntax can be used for rich text representation.
     * </p>
     * @param description a short description of the server variable
     */
    void setDescription(String description);

    /**
     * This method sets the description property of ServerVariable instance to the given description argument and returns the modeified instance.
     * <p>
     * Description property is optional for server variable. CommonMark syntax can be used for rich text representation.
     * </p>
     * @param description a short description of the server variable
     * @return ServerVariable instance with the set description property
     */
    ServerVariable description(String description);

}