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

package org.eclipse.microprofile.openapi.models.examples;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;

/**
 * Example
 * <p>
 * An object containing sample data for the related object.
 * <p>
 * In all cases, the example value is expected to be compatible with the type schema of its associated value. Tooling implementations MAY choose to
 * validate compatibility automatically, and reject the example value(s) if incompatible.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#exampleObject">OpenAPI Specification Example Object</a>
 */
public interface Example extends Constructible, Extensible, Reference<Example> {

    /**
     * Returns the summary property from an Example instance.
     *
     * @return short description of the example
     **/
    String getSummary();

    /**
     * Sets this Example's summary property to the given string.
     *
     * @param summary short description of the example
     */
    void setSummary(String summary);

    /**
     * Sets this Example's summary property to the given string.
     *
     * @param summary short description of the example
     * @return the current Example object
     */
    Example summary(String summary);

    /**
     * Returns the description property from an Example instance.
     *
     * @return long description of the example
     **/
    String getDescription();

    /**
     * Sets this Example's description property to the given string.
     *
     * @param description long description of the example
     */
    void setDescription(String description);

    /**
     * Sets this Example's description property to the given string.
     *
     * @param description long description of the example
     * @return the current Example object
     */
    Example description(String description);

    /**
     * Returns the value property from an Example instance.
     *
     * @return embedded literal example object
     **/
    Object getValue();

    /**
     * Sets this Example's value property to the given value. The value field and externalValue field are mutually exclusive.
     *
     * @param value a literal example object
     */
    void setValue(Object value);

    /**
     * Sets this Example's value property to the given value. The value field and externalValue field are mutually exclusive.
     *
     * @param value a literal example object
     * @return the current Example object
     */
    Example value(Object value);

    /**
     * Returns the externalValue property from an Example instance.
     *
     * @return URL that points to the literal example
     **/
    String getExternalValue();

    /**
     * Sets this Example's externalValue property to the given string. The value field and externalValue field are mutually exclusive.
     *
     * @param externalValue URL that points to the literal example
     */
    void setExternalValue(String externalValue);

    /**
     * Sets this Example's externalValue property to the given string. The value field and externalValue field are mutually exclusive.
     *
     * @param externalValue URL that points to the literal example
     * @return the current Example object
     */
    Example externalValue(String externalValue);

}