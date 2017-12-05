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

import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * This object represents a callback URL that will be invoked.
 **/
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Callbacks.class)
@Inherited
public @interface Callback {
    /**
     * The friendly name used to refer to this callback
     * 
     * @return the name of the callback
     **/
    String name();

    /**
     * An absolute URL which defines the destination which will be called with the supplied operation definition.
     * 
     * @return the callback URL
     */
    String callbackUrlExpression();

    /**
     * The array of operations that will be called out-of band
     * 
     * @return the callback operations
     **/
    Operation[] operation() default {};

    /**
     * Reference value to a Callback object.
     *
     * @return reference to a callback
     **/
    String ref() default "";
}
