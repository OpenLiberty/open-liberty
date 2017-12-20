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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This object represents a Schema of type array, allowing the items to have a Schema annotation inside of this object.
 *
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ArraySchema {

    /**
     * The schema defining the type used for the array.
     *
     * @return the schema of this media type
     **/
    Schema schema() default @Schema;

    /**
     * Sets the maximum number of items in an array.
     * This integer MUST be greater than, or equal to, 0.
     * <p>
     * An array instance is valid against "maxItems" if its size is less than, or equal to, the value of this keyword.
     * </p> 
     * Ignored if value is Integer.MIN_VALUE.
     * 
     * @return the maximum number of items in this array
     **/
    int maxItems() default Integer.MIN_VALUE;

    /**
     * Sets the minimum number of items in an array.
     * This integer MUST be greater than, or equal to, 0. 
     * <p>
     * An array instance is valid against "minItems" if its size is greater than, or equal to, the value of this keyword.
     * </p>
     * Ignored if value is Integer.MAX_VALUE.
     * 
     * @return the minimum number of items in this array
     **/
    int minItems() default Integer.MAX_VALUE;

    /**
     * Determines if the items in the array SHOULD be unique.
     * <p>
     * If false, the instance validates successfully.
     * If true, the instance validates successfully if all of its elements are unique.
     * </p>
     * @return whether the items in this array are unique
     **/
    boolean uniqueItems() default false;
}