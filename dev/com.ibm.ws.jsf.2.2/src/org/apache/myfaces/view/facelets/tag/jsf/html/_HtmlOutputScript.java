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
package org.apache.myfaces.view.facelets.tag.jsf.html;

import javax.faces.component.UIOutput;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * TODO: DOCUMENT ME!
 * <p>
 * This tag is backed using a javax.faces.component.UIOutput component instance.
 * In other words, instances of this component class are created when it is resolved
 * a Resource annotation, so there is no concrete class or specific tag handler for it,
 * but there exists a concrete renderer for it.
 * </p>
 */
@JSFComponent(
        configExcluded=true,
        defaultRendererType="javax.faces.resource.Script")
abstract class _HtmlOutputScript extends UIOutput
{

    /**
     * 
     * @return
     */
    @JSFProperty
    public abstract String getLibrary();
    

    /**
     * 
     * @return
     */
    @JSFProperty(required = true)
    public abstract String getName();

    /**
     * 
     * @return
     */
    @JSFProperty
    public abstract String getTarget();
}
