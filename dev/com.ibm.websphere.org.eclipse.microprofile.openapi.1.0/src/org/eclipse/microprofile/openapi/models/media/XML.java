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

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * A metadata object that allows for more fine-tuned XML model definitions. When using arrays, XML element names are not inferred (for singular/plural
 * forms) and the name property SHOULD be used to add that information.
 * <p>
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#xmlObject">XML Object</a>
 * </p>
 */
public interface XML extends Constructible, Extensible {

    /**
     * This method returns the name property from XML instance.
     * <p>
     * The name property replaces the name of the element/attribute used for the described schema property.
     * </p> 
     * @return String name
     **/
    String getName();

    /**
     * This method sets the name property of XML instance to the given String argument.
     * <p>
     * The name property replaces the name of the element/attribute used for the described schema property.
     * </p>
     * @param name the name of this XML instance
     */
    void setName(String name);

    /**
     * This method sets the name property of XML instance to the given String argument and returns the modified instance.
     * <p>
     * The name property replaces the name of the element/attribute used for the described schema property.
     * </p>
     * @param name the name of this XML instance
     * @return XML instance with the set name property
     */
    XML name(String name);

    /**
     * This method returns the namespace property of XML instance.
     * <p>
     * The namespace property is the URI of the namespace definition. Value MUST be in the form of an absolute URI.
     * </p> 
     * @return String namespace
     **/
    String getNamespace();

    /**
     * This method sets the namespace property of XML instance to the given String argument.
     * <p>
     * The namespace property is the URI of the namespace definition. Value MUST be in the form of an absolute URI.
     * </p> 
     * @param namespace the URI of the namespace definition
     */
    void setNamespace(String namespace);

    /**
     * This method sets the namespace property of XML instance to the given String argument and returns the modified instance.
     * <p>
     * The namespace property is the URI of the namespace definition. Value MUST be in the form of an absolute URI.
     * </p> 
     * @param namespace the URI of the namespace definition
     * @return XML instance with the set namespace property
     */
    XML namespace(String namespace);

    /**
     * This method returns the prefix property of XML instance.
     * <p>
     * This property is a String prefix to be used for the name.
     * </p> 
     * @return String prefix
     **/
    String getPrefix();

    /**
     * This method sets the prefix property of XML instance to the given String argument.
     * <p>
     * This property is a String prefix to be used for the name.
     * </p> 
     * @param prefix string prefix to be used with the name
     */
    void setPrefix(String prefix);

    /**
     * This method sets the prefix property of XML instance to the given String argument and returns the modified instance.
     * <p>
     * This property is a String prefix to be used for the name.
     * </p>
     * @param prefix string prefix to be used with the name
     * @return XML instance with the set prefix property
     */
    XML prefix(String prefix);

    /**
     * This method returns the attribute property of XML instance.
     * <p>
     * Attribute property declares whether the property definition translates to an attribute instead of an element. Default value is FALSE.
     * </p> 
     * @return Boolean attribute
     **/
    Boolean getAttribute();

    /**
     * This method sets the attribute property of XML instance to the given Boolean argument.
     * <p>
     * Attribute property declares whether the property definition translates to an attribute instead of an element. Default value is FALSE.
     * </p> 
     * @param attribute a boolean that declares whether the property definition translates to an attribute instead of an element
     */
    void setAttribute(Boolean attribute);

    /**
     * This method sets the attribute property of XML instance to the given Boolean argument and returns the modified instance.
     * <p>
     * Attribute property declares whether the property definition translates to an attribute instead of an element. Default value is FALSE.
     * </p> 
     * @param attribute a boolean that declares whether the property definition translates to an attribute instead of an element
     * @return XML instance with the set attribute property
     */
    XML attribute(Boolean attribute);

    /**
     * This method returns the wrapped property of XML instance.
     * <p>
     * Wrapped property MAY be used only for an array definition. Signifies whether the array is wrapped. The definition takes effect only when
     * defined alongside type being array. Default value is FALSE.
     * </p> 
     * @return Boolean wrapped
     **/
    Boolean getWrapped();

    /**
     * This method sets the wrapped property of XML instance to the given Boolean argument.
     * <p>
     * Wrapped property MAY be used only for an array definition. Signifies whether the array is wrapped. The definition takes effect only when
     * defined alongside type being array. Default value is FALSE.
     * </p>
     * @param wrapped a boolean that signifies whether the array is wrapped
     */
    void setWrapped(Boolean wrapped);

    /**
     * This method sets the wrapped property of XML instance to the given Boolean argument and returns the modified instance.
     * <p>
     * Wrapped property MAY be used only for an array definition. Signifies whether the array is wrapped. The definition takes effect only when
     * defined alongside type being array. Default value is FALSE.
     * </p> 
     * @param wrapped a boolean that signifies whether the array is wrapped
     * @return XML instance with the set wrapped property
     */
    XML wrapped(Boolean wrapped);

}