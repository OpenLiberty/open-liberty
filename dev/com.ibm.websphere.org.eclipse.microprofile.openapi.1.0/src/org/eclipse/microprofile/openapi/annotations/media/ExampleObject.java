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

package org.eclipse.microprofile.openapi.annotations.media;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This object illustrates an example of a particular content
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#exampleObject">OpenAPI Specification Example Object</a>
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExampleObject {
    /**
     * A unique name to identify this particular example in a map.
     * <p>
     * The name is REQUIRED when the example is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}. The 
     * name will be used as the key to add this example to the 'examples' map for reuse.
     * </p>
     * 
     * @return the name of this example
     **/
    String name() default "";

    /**
     * A brief summary of the purpose or context of the example
     * 
     * @return a summary of this example
     **/
    String summary() default "";

    /**
     * Long description for the example. 
     * CommonMark syntax MAY be used for rich text representation.
     * 
     * @return a description of this example
     **/
    String description() default "";

    /**
     * A string representation of the example. 
     * <p>
     * This is mutually exclusive with the externalValue property, and ignored if the externalValue property is specified. 
     * </p>
     * If the media type associated with the example allows parsing into an object, it may be converted from a string.
     * @return the value of the example
     **/
    String value() default "";

    /**
     * A URL to point to an external document to be used as an example.
     * This provides the capability to reference examples that cannot easily be included in JSON or YAML documents.
     * <p> 
     * This is mutually exclusive with the value property.
     * </p>
     * @return an external URL of the example
     **/
    String externalValue() default "";

    /**
     * Reference value to an Example object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to an example
     **/
    String ref() default "";

}
