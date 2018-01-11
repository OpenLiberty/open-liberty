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

package org.eclipse.microprofile.openapi.annotations.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Tags annotation is a container for @Tag annotations. When used on a method or a type
 * it is treated as if each tag annotation were applied individually.
 * <p>
 * <b>Note:</b> If both {@link org.eclipse.microprofile.openapi.annotations.tags.Tag Tag} and 
 * {@link org.eclipse.microprofile.openapi.annotations.tags.Tags Tags} annotations are specified on the same method/class,
 * then both tag definitions should be applied to method/class.
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#tagObject">OpenAPI Specification Tag Object</a>
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Tags {
    /**
     * An array of Tag annotation objects which hold metadata for the API
     *
     * @return an array of Tag annotations
     */
    Tag[] value() default {};

    /**
     * An array of referenced Tag objects which hold metadata for the API
     *
     * @return an array of references to tags
     **/
    String[] refs() default "";
}
