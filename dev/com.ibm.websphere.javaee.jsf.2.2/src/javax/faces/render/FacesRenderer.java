/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.render;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
public @interface FacesRenderer
{
    /**
     * The value of this annotation attribute is taken to be the <i>component-family</i> which, in combination with
     * {@link #rendererType()} can be used to obtain a reference to an instance of this {@link Renderer} by calling
     * {@link RenderKit#getRenderer(java.lang.String, java.lang.String)}.
     */
    public String componentFamily();

    /**
     * The value of this annotation attribute is taken to be the <i>renderer-type</i> which, in combination with
     * {@link #componentFamily()} can be used to obtain a reference to an instance of this {@link Renderer} by calling
     * {@link RenderKit#getRenderer(java.lang.String, java.lang.String)}.
     */
    public String rendererType();

    /**
     * The value of this annotation attribute is taken to be the <i>render-kit-id</i> in which an instance of this class
     * of {@link Renderer} must be installed.
     */
    public String renderKitId() default RenderKitFactory.HTML_BASIC_RENDER_KIT;
}
