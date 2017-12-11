/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.openapi;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;

/**
 * This class allows application developers to build new OpenAPI model elements.  
 * 
 * <br><br>For example, to start a new top-level OpenAPI element with an ExternalDocument inside of it an application developer would write:
 * 
 * <pre><code>OASFactory.createObject(OpenAPI.class)
 *          .setExternalDocs(OASFactory.createObject(ExternalDocumentation.class).url("http://myDoc"));</code></pre>
 */
public final class OASFactory {
    
    private OASFactory() {}

    /**
     * This method creates a new instance of a constructible element from the OpenAPI model tree.
     *
     * <br><br>Example:
     * <pre><code>OASFactory.createObject(Info.class).title("Airlines").description("Airlines APIs").version("1.0.0");
     * </code></pre>
     * @param <T> describes the type parameter
     * @param clazz represents a model which extends the {@link org.eclipse.microprofile.openapi.models.Constructible} interface
     *
     * @return a new instance of the requested model class
     * 
     * @throws NullPointerException if the specified class is null
     * @throws IllegalArgumentException if an instance could not be created, most likely, due to an illegal or inappropriate class
     */
    public static <T extends Constructible> T createObject(Class<T> clazz) {
        return OASFactoryResolver.instance().createObject(clazz);
    }

}