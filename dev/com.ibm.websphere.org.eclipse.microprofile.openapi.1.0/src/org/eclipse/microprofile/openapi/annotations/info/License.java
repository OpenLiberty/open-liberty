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

package org.eclipse.microprofile.openapi.annotations.info;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * License information for the exposed API.
 * 
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#licenseObject"
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface License {
    /**
     * The license name used for the API.
     * 
     * @return the name of the license
     **/
    String name();

    /**
     * A URL to the license used for the API. MUST be in the format of a URL.
     * 
     * @return the URL of the license
     **/
    String url() default "";

}
