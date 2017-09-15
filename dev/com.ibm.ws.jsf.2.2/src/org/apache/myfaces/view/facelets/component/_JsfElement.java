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
package org.apache.myfaces.view.facelets.component;

import javax.faces.component.UIPanel;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * 
 * @author Leonardo Uribe
 */
@JSFComponent(name="jsf:element",
    clazz = "org.apache.myfaces.view.facelets.component.JsfElement",template=true,
    implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
    defaultEventName = "click",
    defaultRendererType="javax.faces.passthrough.Element")
abstract class _JsfElement extends UIPanel implements 
    _EventProperties, _FocusBlurProperties, _ChangeSelectProperties, _StyleProperties
{
    public static final String COMPONENT_FAMILY = "javax.faces.Panel";
    public static final String COMPONENT_TYPE = "oam.passthrough.Element";
    
    /**
     * HTML: Script to be invoked when the page is loaded
     * 
     */
    @JSFProperty(clientEvent="load")
    public abstract String getOnload();

    /**
     * HTML: Script to be invoked when the page is unloaded
     * 
     */
    @JSFProperty(clientEvent="unload")
    public abstract String getOnunload();

}
