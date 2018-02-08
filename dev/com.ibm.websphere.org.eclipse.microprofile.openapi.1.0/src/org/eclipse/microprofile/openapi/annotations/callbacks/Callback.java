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

package org.eclipse.microprofile.openapi.annotations.callbacks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This object represents a callback URL that will be invoked.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#callbackObject">OpenAPI Specification Callback Object</a>
 **/
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Callbacks.class)
@Inherited
public @interface Callback {
    /**
     * The friendly name used to refer to this callback. It is a REQUIRED property unless this is only a reference to a callback.
     * <p>
     * The name is REQUIRED when the callback is defined within {@link org.eclipse.microprofile.openapi.annotations.Components}. The 
     * name will be used as the key to add this callback to the 'callbacks' map for reuse.
     * </p>
     * 
     * @return the name of this callback
     **/
    String name() default "";

    /**
     * An absolute URL which defines the destination which will be called with the supplied operation definition.
     * <p>
     * It is a REQUIRED property unless this is only a reference to a callback instance.
     * </p>
     * 
     * @return the callback URL
     */
    String callbackUrlExpression() default "";

    /**
     * The array of operations that will be called out-of band
     * 
     * @return the callback operations
     **/
    CallbackOperation[] operations() default {};

    /**
     * Reference value to a Callback object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     * 
     * @return reference to a callback object definition
     **/
    String ref() default "";
}
