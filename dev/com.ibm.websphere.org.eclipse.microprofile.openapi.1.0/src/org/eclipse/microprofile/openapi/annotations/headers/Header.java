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

package org.eclipse.microprofile.openapi.annotations.headers;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Describes a single header object
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#header-object">OpenAPI Specification Header Object</a>
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Header {
    /**
     * The name of the header. The name is only used as the key to add this header to a map.
     * <p>
     * It is a REQUIRED property unless this is only a reference to a header instance.
     * </p>
     * When the header is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}, 
     * the name will be used as the key to add this header to the 'headers' map for reuse.
     * 
     * @return this header's name
     **/
    String name() default "";

    /**
     * Additional description data to provide on the purpose of the header
     *
     * @return the header's description
     **/
    String description() default "";

    /**
     * The schema defining the type used for the header. Ignored if the properties content or array are specified.
     *
     * @return the schema of the header
     **/
    Schema schema() default @Schema();

    /**
     * Determines whether this header is mandatory. The property may be included and its default value is false.
     *
     * @return whether or not the header is required
     **/
    boolean required() default false;

    /**
     * Specifies that a header is deprecated and should be transitioned out of usage.
     *
     * @return whether or not the header is deprecated
     **/
    boolean deprecated() default false;

    /**
     * When true, allows sending an empty value. If false, the header will be considered \&quot;null\&quot; if no value is present. This may create
     * validation errors when the header is required.
     *
     * @return whether or not the header allows empty values
     **/
    boolean allowEmptyValue() default false;

    /**
     * Reference value to a Header object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     * 
     * @return reference to a header
     **/
    String ref() default "";

}
